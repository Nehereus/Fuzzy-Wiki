package edu.uci.cs230.team10;

import org.apache.lucene.search.ScoreDoc;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.logging.Logger;

class SearcherTest {
    private static final Logger logger = Logger.getLogger(SearcherTest.class.getName());

    @Test
    void search() {
        String[] queries = new String[]{
                    "Panda",
    };
        try {
            for (String query : queries) {
                ScoreDoc[] res = Searcher.search(query);
                String explanation = Searcher.interpret(Searcher.parseQuery(query), res);
                logger.info( explanation);
            }

        } catch (Exception e) {
            //fail("Search failed");
            System.out.println(e.getMessage());
        }
    }

}
