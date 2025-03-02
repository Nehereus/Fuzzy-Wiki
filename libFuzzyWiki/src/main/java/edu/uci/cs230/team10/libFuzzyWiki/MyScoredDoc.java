package edu.uci.cs230.team10.libFuzzyWiki;

public final class MyScoredDoc {
    public final String title;
    public final float score;
    public final String text;

    public MyScoredDoc(String title, float score, String text) {
        this.title = title;
        this.score = score;
        this.text = text;
    }
}
