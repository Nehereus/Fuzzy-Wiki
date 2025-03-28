package edu.uci.cs230.team10.FuzzyWikiApp;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import edu.uci.cs230.team10.libFuzzyWiki.Searcher;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Server {
    /**
     * the simplest implementation of the required APIs
     */
    Config config;
    //modified to protected for testing purposes
    WikiSearcher wikiSearcher;
    private int port;
    private final Logger logger = Logger.getLogger(Server.class.getName());

    public Server() {
        // important: pass -Dconfig.file=<application.conf> to the JVM
        config = ConfigFactory.load();
        //should verify the config file here
        parseConfig();
        Javalin app = Javalin.create();
        app.get("/search", this::searchHandler);
        app.get("/document/{title}", this::documentHandler);
        app.start(port);
    }

    private void documentHandler(@NotNull Context ctx) {
        String title = URLDecoder.decode(ctx.pathParam("title"), StandardCharsets.UTF_8);
        boolean forwarding = Boolean.parseBoolean(ctx.queryParam("forwarding"));
        logger.info("getting document for " + title);
        JSONObject res = new JSONObject();
        try {
            if (forwarding) {
                res = wikiSearcher.getArticleByTitleOrForward(title);
            } else {
                res = wikiSearcher.getArticleByTitle(title);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.result("Document "+ title+ " not found");
            ctx.status(404);
            logger.warning("Document "+ title+ " not found");
        }

        if (!res.isEmpty()) {
            ctx.result(res.toString());
            ctx.status(200);
        } else {
            ctx.result("Document "+ title+ " not found");
            ctx.status(404);
            logger.warning("Document "+ title+ " not found");
        }
    }

    private void searchHandler(@NotNull Context ctx) throws QueryNodeException, IOException {
        String query = ctx.queryParam("query");
        boolean forwarding = Boolean.parseBoolean(ctx.queryParam("forwarding"));
        logger.info("searching for " + query + " forwarding " + forwarding);
        if (query == null) {
            ctx.result("query parameter is missing");
            ctx.status(400);
            return;
        }
        if (forwarding) {
            var res = "";
            try {
                res = wikiSearcher.searchForwardMerge(query).toString();
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500);
            }
            ctx.result(res);
        } else {
            ctx.result(wikiSearcher.search(query).toJson().toString());
        }
        ctx.status(200);
    }

    //utility function used to parse config file
    private void parseConfig() {
        port = config.getInt("port");
        Node node1 = new Node(config.getString("name"), config.getString("addr"), config.getInt("port"), config.getIntList("shards"));
        logger.info("loading config file: Node:" + node1.getName() + " addr:" + node1.getAddr() + " shards:" + node1.getShards());
        List<Node> peers = config.getConfigList("peers").stream()
                .map(node -> new Node(node.getString("name"), node.getString("addr"), node.getInt("port"), node.getIntList("shards")))
                .collect(Collectors.toList());
        wikiSearcher = new WikiSearcher(new Searcher(Path.of(config.getString("indexPath"))), node1, peers, config.getInt("totalShards"));
    }

    public static void main(String[] args) {
        new Server();
    }
}
