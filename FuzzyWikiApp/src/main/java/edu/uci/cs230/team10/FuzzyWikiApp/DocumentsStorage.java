package edu.uci.cs230.team10.FuzzyWikiApp;
import edu.uci.cs230.team10.libFuzzyWiki.MyScoredDoc;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DocumentsStorage {
    // A extremely simple implementation of "DB" for storing title-original_text pairs

    private final static Map<String, String> titleTextMap = new HashMap<>();

    public static void put(MyScoredDoc doc) {
        titleTextMap.put(doc.title, doc.text);
    }

    /**
     * get a document's original text from the storage
     * @param title the title of the document
     * @return the text of the document or null if not found
     */
    public static String get(String title)  {
        return titleTextMap.get(title);
    }

    public static JSONObject getJson(String title)  {
        if (!titleTextMap.containsKey(title)) {
            return null;
        }
        return new JSONObject().put("title", title).put("text", get(title));
    }

    public static void putAll(List<MyScoredDoc> docs) {
        for (MyScoredDoc doc : docs) {
            put(doc);
        }
    }


}
