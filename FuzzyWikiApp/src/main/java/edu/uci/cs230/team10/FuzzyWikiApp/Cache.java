package edu.uci.cs230.team10.FuzzyWikiApp;

import edu.uci.cs230.team10.libFuzzyWiki.MyScoredDoc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cache {
    private final static Map<String, List<MyScoredDoc>> cache = new HashMap<>();

    public static void put(String query, List<MyScoredDoc> docs) {
        cache.put(query, docs);
    }

    /**
     * get a document's original text from the storage
     * @param query the title of the document
     * @return the text of the document or null if not found
     */
    public static List<MyScoredDoc> get(String query) {
        return cache.getOrDefault(query, null);
    }

    public static boolean contains(String query) {
        return cache.containsKey(query);
    }

}
