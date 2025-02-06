package edu.uci.cs230.team10;

import org.apache.lucene.search.ScoreDoc;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SearcherTest {
    private static final Logger logger = Logger.getLogger(SearcherTest.class.getName());
    @Test
    void search() {
        Searcher searcher = new Searcher();
        try {
            var titleTest = searcher.search("water");
            var fuzzyTitleTest = searcher.search("Anacism");
            var textTest = searcher.search("United States");
            var fuzzyTextTest = searcher.search("uinted states");

            logger.info("Title search result: " + Arrays.toString(titleTest));
            logger.info("Fuzzy title search result: " + Arrays.toString(fuzzyTitleTest));
            logger.info("Text search result: " + Arrays.toString(textTest));
            logger.info("Fuzzy text search result: " + Arrays.toString(fuzzyTextTest));

            assertNotEquals(new ScoreDoc[]{}, titleTest);
        } catch (Exception e) {
            fail("Search failed");
        }
    }
}
