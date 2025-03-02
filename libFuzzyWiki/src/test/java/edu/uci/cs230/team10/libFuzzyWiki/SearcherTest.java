package edu.uci.cs230.team10.libFuzzyWiki;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;


class SearcherTest {
    Searcher searcher;
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        searcher=new Searcher(Path.of("/tmp/luceneIndex"));
    }
    @Test
    void testSearchForMerge() throws QueryNodeException, IOException {
            System.out.println(searcher.searchForMerge("google"));
    }
}
