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
    Server s = new Server();
    Logger logger = Logger.getLogger(WikiSearcher.class.getName());
    @BeforeAll
    static void setUp() {
    }


    @Test
    void searchForwardMerge() throws QueryNodeException, IOException {
        s.wikiSearcher.searchForwardMerge("google");
    }

    @Test
    void search() throws QueryNodeException, IOException {

    }
}
