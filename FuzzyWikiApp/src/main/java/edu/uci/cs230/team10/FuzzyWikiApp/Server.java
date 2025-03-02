package edu.uci.cs230.team10.FuzzyWikiApp;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.javalin.Javalin;
import edu.uci.cs230.team10.libFuzzyWiki.Searcher;
import io.javalin.http.Context;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Server {
    /**
     * the simplest implementation of the required APIs
     */
    Config config;
    private Node node;
    private List<Node> peers;
    private WikiSearcher wikiSearcher;
    private final Javalin app;
    private final int port = 8084;
    private final Logger logger = Logger.getLogger(Server.class.getName());

    public Server() {
        // important: pass -Dconfig.file=<application.conf> to the JVM
        config = ConfigFactory.load();
        //should verify the config file here
        parseConfig();
        app = Javalin.create();
        app.get("/search/{query}{forwarding}", this::searchHandler);
        app.get("/document/{title}", this::documentHandler);
        app.start(port);
    }

    private void documentHandler(@NotNull Context context) throws FileNotFoundException {
        String title = context.pathParam("title");
        logger.info("getting document for " + title);
        try{
            JSONObject res = DocumentsStorage.getJson(title);
            context.result(res.toString());
            context.status(200);
        }catch (FileNotFoundException e){
            context.result("Document not found");
            context.status(404);
            logger.warning("Document not found");
            return;
        }
    }

    private void searchHandler(Context ctx) throws QueryNodeException, IOException {
        String query = ctx.queryParam("query");
        boolean forwarding = Boolean.parseBoolean(ctx.queryParam("forwarding"));
        logger.info("searching for " + query+" forwarding "+forwarding);
        if (query == null) {
            ctx.result("query parameter is missing");
            ctx.status(400);
            return;
        }else{
            ctx.status(200);
        }
        if(forwarding){
            ctx.result(wikiSearcher.searchForwardMerge(query).toString());
        }else {
            ctx.result(wikiSearcher.search(query).toJson().toString());
        }
    }
    //utility function used to parse config file
    private void parseConfig() {
        node = new Node(config.getString("name"), config.getString("addr"), config.getIntList("shards"));
       peers= config.getConfigList("nodes").stream()
                .map(node -> new Node(node.getString("name"), node.getString("addr"), node.getIntList("shards")))
                .collect(Collectors.toList());

        wikiSearcher= new WikiSearcher(new Searcher(Path.of(config.getString("indexPath"))),node, peers,config.getInt("totalShards"));
    }

    public static void main(String[] args) {
        new Server();
    }
}
