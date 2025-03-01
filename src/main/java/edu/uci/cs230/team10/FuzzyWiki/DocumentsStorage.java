package edu.uci.cs230.team10.FuzzyWiki;

import edu.uci.cs230.team10.libFuzzyWiki.MyScoredDoc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DocumentsStorage {
    // A extremely simple implementation of "DB" for storing title-original_text pairs

    private final static Map<String, String> titleTextMap = new HashMap<>();
    /**
     * put a document's original text into the storage
     * @param title the title of the document
     * @param text the text of the document
     * @return whether there is already a document with the same title
     */
    public static boolean put(String title, String text) {
        if (titleTextMap.containsKey(title)) {
            return false;
        }
        titleTextMap.put(title, text);
        return true;
    }

    /**
     * get a document's original text from the storage
     * @param title the title of the document
     * @return the text of the document
     */
    public static String get(String title) {
        return titleTextMap.get(title);
    }

    public static void putAll(List<MyScoredDoc> docs) {
        for (MyScoredDoc doc : docs) {
            titleTextMap.put(doc.title, doc.text);
        }
    }
}
