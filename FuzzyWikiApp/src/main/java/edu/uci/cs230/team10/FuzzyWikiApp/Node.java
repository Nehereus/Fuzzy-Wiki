package edu.uci.cs230.team10.FuzzyWikiApp;
import java.util.List;

public class Node {
    private final String name;
    private final String addr;
    private final List<Integer> shards;


    private final int port;

    public String getName() {
        return name;
    }

    public String getAddr() {
        return addr;
    }

    public List<Integer> getShards() {
        return shards;
    }

    public int getPort() {
        return port;
    }

    public Node(String name, String addr,int port, List<Integer> shards) {
        this.name = name;
        this.addr = addr;
        this.shards = shards;
        this.port = port;
    }
}
