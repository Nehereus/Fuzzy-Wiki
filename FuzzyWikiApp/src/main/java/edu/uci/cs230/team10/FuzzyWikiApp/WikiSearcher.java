package edu.uci.cs230.team10.FuzzyWikiApp;
import edu.uci.cs230.team10.libFuzzyWiki.DocTermInfo;
import edu.uci.cs230.team10.libFuzzyWiki.DocTermInfoHandler;
import edu.uci.cs230.team10.libFuzzyWiki.MyScoredDoc;
import edu.uci.cs230.team10.libFuzzyWiki.Searcher;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.json.JSONObject;

import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WikiSearcher implements AutoCloseable {
    private final List<Node> selectedPeers;
    private final Node node;
    private final Searcher searcher;
    private final CloseableHttpAsyncClient httpClient;
    //shards are zero-indexed
    private final Logger logger = Logger.getLogger(WikiSearcher.class.getName());

    /**
     * Constructor for WikiSearcher
     * @param searcher the searcher object
     * @param peers the list of peer nodes except the host itself
     */
    public WikiSearcher(Searcher searcher,Node node, List<Node> peers, int totalShards) {
        this.node = node;
        this.selectedPeers = selectPeers(peers, totalShards);
        this.searcher = searcher;
        this.httpClient = HttpAsyncClients.createDefault();
        this.httpClient.start();
    }

    //a meta search function that search locally, forward requests, and merges the search results from all nodes and ranks them
    public List<JSONObject> searchForwardMerge(String query) throws IOException, QueryNodeException {
        DocTermInfo localRes = search(query);
        // needs to come up with an algorithm to select the nodes based on the index shards they held to achieve full index coverage
       List<DocTermInfo> forwardRes = forward(query, selectedPeers);
       forwardRes.add(localRes);
       List<MyScoredDoc> res = DocTermInfoHandler.mergeAndRank(forwardRes);
       DocumentsStorage.putAll(res);
       return res.stream().map(MyScoredDoc::toJsonPreview).collect(Collectors.toList());
    }

    //This is essentially set cover problem, which is NP-hard, but considering the rarity
    //of the case where the number of shards is large, we use brute force to solve the problem
    private List<Node> selectPeers(List<Node> peers, int totalShards) {
        List<Node> res = new ArrayList<>();
        List<Integer> remainingShards = IntStream.range(0, totalShards)
                .boxed()
                .collect(Collectors.toList());
        remainingShards.removeAll(node.getShards());
        while (!remainingShards.isEmpty()) {
            Node bestPeer = null;
            int bestCount = 0;
            for (Node peer : peers) {
                int count = 0;
                for (int shard : peer.getShards()) {
                    if (remainingShards.contains(shard)) {
                        count++;
                    }
                }
                if (count > bestCount) {
                    bestCount = count;
                    bestPeer = peer;
                }
            }
            if (bestPeer != null) {
                remainingShards.removeAll(bestPeer.getShards());
                res.add(bestPeer);
                logger.info("selecting peer: " + bestPeer.getName());
            } else {
                logger.warning("No peer can cover the remaining shards, check the shards configuration");
                break;
            }
        }

        return res;
    }
    //the search function used for local search
    public DocTermInfo search(String query) throws IOException, QueryNodeException, RuntimeException {
        logger.info("Accepting task for query: " + query);
        return searcher.searchForMerge(query);
    }

    private List<DocTermInfo> forward(String query, List<Node> nodes) {
        List<DocTermInfo> res= new ArrayList<>();
        CompletableFuture<?>[] futuresArray = nodes.stream()
                .map(node -> {
                    URI url ;
                    try {
                        // URL encode the query parameter to handle spaces and special characters
                        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                        url = new URI(String.format("http://%s/search?query=%s&forwarding=false",
                                node.getAddr()+":"+node.getPort(), encodedQuery));
                    } catch (URISyntaxException e) {
                        logger.severe("Invalid URI: " + e.getMessage());
                        throw new RuntimeException(e);
                    }

                    SimpleHttpRequest request = new SimpleHttpRequest("GET", url);
                    CompletableFuture<Void> future = new CompletableFuture<>();

                    URI finalUrl = url;
                    httpClient.execute(request, new FutureCallback<>() {
                        // the search api should return a json object representing a DocTermInfo object
                        @Override
                        public void completed(SimpleHttpResponse response) {
                            logger.info("Response from " + finalUrl + ": " + response.getBody());
                            try {
                                var temp = DocTermInfo.from(new JSONObject(response.getBodyText()));
                                res.add(temp);
                            } catch (Exception e) {
                                logger.severe("Error parsing response from " + finalUrl + ": " + e.getMessage());
                                future.completeExceptionally(e);
                                return;
                            }
                            future.complete(null);
                        }

                        @Override
                        public void failed(Exception ex) {
                            logger.severe("Request to " + finalUrl + " failed: " + ex.getMessage());
                            future.completeExceptionally(ex);
                        }

                        @Override
                        public void cancelled() {
                            logger.warning("Request to " + finalUrl + " was cancelled");
                            future.cancel(true);
                        }
                    });

                    return future;
                })
                .toArray(CompletableFuture[]::new);

        // Wait for all requests to complete
        try {
            CompletableFuture.allOf(futuresArray).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Error waiting for requests to complete: " + e.getMessage());
            throw new RuntimeException("Error executing search requests", e);
        }
        return res;
    }
    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
