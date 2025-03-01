package edu.uci.cs230.team10.FuzzyWiki;
import com.typesafe.config.Config;
import io.javalin.Javalin;
import edu.uci.cs230.team10.libFuzzyWiki.Searcher;
import com.typesafe.config.ConfigFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Server {
    /**
     * the simplest implementation of the required APIs
     */
    Config config;
    private Node node;
    private List<Node> peers;
    private WikiSearcher wikiSearcher;
    private Javalin app;
    private int port = 8084;

    public Server() {
        //important: pass -Dconfig.file=<application.conf> to the JVM
        config = ConfigFactory.load();
        // should verify the config file here
        app = Javalin.create().start(config.getInt("port"));
        app.get("/search/{query}{forwarding}", ctx -> {
            String query = ctx.queryParam("query");

        });
    }
    //utility function used to parse config file
    private void parseConfig() {
        node = new Node(config.getString("name"), config.getString("addr"), config.getIntList("shards"));
       peers= config.getConfigList("nodes").stream()
                .map(node -> new Node(node.getString("name"), node.getString("addr"), node.getIntList("shards")))
                .collect(Collectors.toList());

        wikiSearcher= new WikiSearcher(new Searcher(Path.of(config.getString("indexPath"))),node, peers,config.getInt("totalShards"));
    }
}
