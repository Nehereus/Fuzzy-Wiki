package edu.uci.cs230.team10.FuzzyWiki;

import java.util.List;

public class Node {
    private final String name;
    private final String addr;
    private final List<Integer> shards;

    public String getName() {
        return name;
    }

    public String getUrl() {
        return addr;
    }

    public List<Integer> getShards() {
        return shards;
    }

    public Node(String name, String addr, List<Integer> shards) {
        this.name = name;
        this.addr = addr;
        this.shards = shards;
    }
}
