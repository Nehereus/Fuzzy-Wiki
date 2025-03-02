package edu.uci.cs230.team10.libFuzzyWiki;

import org.json.JSONObject;

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
        res.put("text", text!=null?text.substring(0, Math.min(text.length(), text.indexOf('.')+1)):"EMPTY");
        return res;
    }
}
