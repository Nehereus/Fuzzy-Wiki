package edu.uci.cs230.team10.libFuzzyWiki;

import org.json.JSONObject;

import java.util.Objects;

public final class MyScoredDoc {
    public final String title;
    public final float score;
    public final String text;

    public MyScoredDoc(String title, float score, String text) {
        this.title = title;
        this.score = score;
        this.text = text;
    }

    // the returned text will be truncated to the end of first sentence or the length of the text, whichever is shorter
    public JSONObject toJsonPreview() {
        JSONObject res = new JSONObject();
        res.put("title", title);
        res.put("score", score);
        res.put("text",textBrief(text));
        return res;
    }

    // I know this is a terrible code style, inconsistent API but have to do. Life
    public JSONObject toJsonArticle() {
        JSONObject res = new JSONObject();
        res.put("title", title);
        res.put("text", Objects.requireNonNullElse(text, "No Description"));
        return res;
    }
    private String textBrief(String text) {
        try {
            // in case of bugs
            if (text != null) {
                int start = 0;
                int end = text.contains(".") ? text.indexOf(".") : text.length();
                while (start < end && text.substring(start, end).contains("|")) {
                    start = end + 1;
                    end = text.indexOf(".", start);
                    if (end == -1) {
                        end = text.length();
                    }
                    if (start >= end) {
                        return "No Description";
                    }
                }
                return text.substring(start, end);
            }
            return "No Description";
        } catch (IndexOutOfBoundsException e) {
            return "Internal Error: " + e.getMessage();
        }
    }

}
