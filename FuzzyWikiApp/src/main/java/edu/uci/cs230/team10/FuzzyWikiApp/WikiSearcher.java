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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

    // remove invalid documents from result
    public void filerInvalidDocs(List<DocTermInfo> list) throws QueryNodeException, IOException {
        // remove invalid documents
        // First kind: text of document is "REDIRECT {title}"
        // But the title is not exist in the indexing system
        // We can search the title in the indexing system
        // If the title is not exist, we can remove the document
        for(DocTermInfo docTermInfo : list) {
            List<String> toRemove = new ArrayList<>();
            for(String title : docTermInfo.textMap.keySet()) {
                if(docTermInfo.textMap.get(title).startsWith("REDIRECT")) {
                    // remove the "REDIRECT " prefix
                    String redirectTitle = docTermInfo.textMap.get(title).substring(9);
                    // in case some pages are not pure redirect pages
                    // no document has a title longer than 50 characters, right?
                    if(redirectTitle.length()<50&&this.getArticleByTitleOrForward(redirectTitle).isEmpty()) {
                        toRemove.add(title);
                    }
                }
            }
            for(String title : toRemove) {
                docTermInfo.textMap.remove(title);
                docTermInfo.infoMap.remove(title);
            }
        }

    }

    //a meta search function that search locally, forward requests, and merges the search results from all nodes and ranks them
    public List<JSONObject> searchForwardMerge(String query) throws IOException, QueryNodeException,CompletionException {
        long startTime = System.nanoTime();
        CompletableFuture<DocTermInfo> localSearchFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return search(query);
            } catch (IOException | QueryNodeException e) {
                throw new CompletionException(e);
            }
        });

        CompletableFuture<List<DocTermInfo>> forwardSearchFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return forward(query, selectedPeers);
            } catch (RuntimeException e) {
                throw new CompletionException(e);
            }
        });

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(localSearchFuture, forwardSearchFuture);

        try {
            allFutures.join();
            DocTermInfo localRes = localSearchFuture.getNow(null);
            List<DocTermInfo> forwardRes = forwardSearchFuture.getNow(Collections.emptyList());
            forwardRes.add(localRes);
            int searchTime = (int) ((System.nanoTime() - startTime) / 1_000_000);
            filerInvalidDocs(forwardRes);   // remove invalid documents
            startTime = System.nanoTime();
            List<MyScoredDoc> res = DocTermInfoHandler.mergeAndRank(forwardRes);
            DocumentsStorage.putAll(res);
            List<JSONObject> jsonRes = res.stream()
                    .map(MyScoredDoc::toJsonPreview)
                    .collect(Collectors.toList());
            int mergeTime = (int) ((System.nanoTime() - startTime) / 1_000_000);
            jsonRes.add(new JSONObject()
                    .put("totalTimeUsed", searchTime + mergeTime).
                    put("searchTimeUsed", searchTime).
                    put("mergeTimeUsed", mergeTime));

            return jsonRes;
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause instanceof QueryNodeException) {
                throw (QueryNodeException) cause;
            } else {
                throw e;
            }
        }
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

    public JSONObject getArticleByTitleOrForward(String title) throws IOException, QueryNodeException {
        // First, try to get the article locally
        JSONObject localResult = getArticleByTitle(title);
        if (!localResult.isEmpty()) {
            return localResult;
        }

        // If not found locally, forward the request to remote nodes
        List<CompletableFuture<JSONObject>> futuresList = selectedPeers.stream()
                .map(node -> {
                    URI url;
                    try {
                        url = new URI(String.format("http://%s/document/%s?forwarding=false",
                                node.getAddr() + ":" + node.getPort(), URLEncoder.encode(title, StandardCharsets.UTF_8)));
                    } catch (URISyntaxException e) {
                        logger.severe("Invalid URI: " + e.getMessage());
                        throw new RuntimeException(e);
                    }

                    SimpleHttpRequest request = new SimpleHttpRequest("GET", url);
                    CompletableFuture<JSONObject> future = new CompletableFuture<>();

                    URI finalUrl = url;
                    httpClient.execute(request, new FutureCallback<>() {
                        @Override
                        public void completed(SimpleHttpResponse response) {
                            logger.info("Response from " + finalUrl + ": " + response.getBody());
                            try {
                                JSONObject jsonResponse = new JSONObject(response.getBodyText());
                                future.complete(jsonResponse);
                            } catch (Exception e) {

                                logger.severe("Error parsing response from " + finalUrl + ": " + e.getMessage() +
                                        " Response: " + response.getBodyText());
                                future.completeExceptionally(e);
                            }
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
                .collect(Collectors.toList());

        // Wait for all requests to complete and return the first non-empty result
        for (CompletableFuture<JSONObject> future : futuresList) {
            try {
                JSONObject result = future.get();
                if (!result.isEmpty()) {
                    return result;
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.severe("Error waiting for requests to complete: " + e.getMessage());
            }
        }

        // If no non-empty result is found, return an empty JSON object
        return new JSONObject();
    }

    /*
     * This function is used to get the document by title, if the document is not found in the local storage,
     * it will search for the document in the index and return it. If not found in the local index, it will
     * return an empty JSON object.
     */

    public JSONObject getArticleByTitle(String title) throws IOException, QueryNodeException {
        JSONObject res = DocumentsStorage.getJson(title);
        if (res == null) {
            var tmep = searcher.getByTitle(title);
            if (tmep != null) {
                res = tmep.toJsonArticle();
            } else {
                res = new JSONObject();
            }

        }
        return res;
    }



    //the search function used for local search
    public DocTermInfo search(String query) throws IOException, QueryNodeException, RuntimeException {
        logger.info("Accepting task for query: " + query);
        long startTime = System.nanoTime();
        DocTermInfo res = searcher.searchForMerge(query);
        DocumentsStorage.putAll(DocTermInfoHandler.mergeAndRank(Collections.singletonList(res)));
        int timeUsed = (int) ((System.nanoTime() - startTime) / 1_000_000);  // Convert to int (ms)
        res.setTimeUsed(timeUsed);
        return res;
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
                                logger.severe("Error parsing response from " + finalUrl + ": " + e.getMessage()+
                                        " Response: " + response.getBodyText());
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
