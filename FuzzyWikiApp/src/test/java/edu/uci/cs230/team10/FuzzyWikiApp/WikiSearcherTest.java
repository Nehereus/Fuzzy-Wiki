package edu.uci.cs230.team10.FuzzyWikiApp;

import edu.uci.cs230.team10.libFuzzyWiki.Searcher;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


class WikiSearcherTest {
    static WikiSearcher wikiSearcher = new WikiSearcher(new Searcher(Path.of("/tmp/luceneIndex"))
            ,new Node("master","127.0.0.1",8080, List.of(1)),new ArrayList<Node>(),1);
    Logger logger = Logger.getLogger(WikiSearcher.class.getName());
    @BeforeAll
    static void setUp() {
    }


    @Test
    void searchForwardMerge() {
    }

    @Test
    void search() throws QueryNodeException, IOException {
       logger.info( wikiSearcher.searchForwardMerge("google").toString());

    }
}
