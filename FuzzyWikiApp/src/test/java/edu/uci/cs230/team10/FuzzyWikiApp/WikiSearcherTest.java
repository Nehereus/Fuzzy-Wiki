package edu.uci.cs230.team10.FuzzyWikiApp;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


class WikiSearcherTest {
    Server s = new Server();
    Logger logger = Logger.getLogger(WikiSearcher.class.getName());

    @BeforeAll
    static void setUp() {
    }


    @Test
    void searchForwardMergeWithFailure() throws QueryNodeException, IOException {
        logger.log(Level.ALL, "Test starts: ");
        for (JSONObject j: s.wikiSearcher.searchForwardMerge("tiger")) {
            System.out.println(j.toString());
        }
    }

    @Test
    void search() throws QueryNodeException, IOException {

    }
}
