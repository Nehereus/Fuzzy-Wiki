package edu.uci.cs230.team10.libFuzzyWiki;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;

class MyCollectionStatisTics {

}

// Attention, CollectionStatistics and TermStatistics values cannot be changed after they are set.
public class SearchResult {
    private final CollectionStatistics collectionStats;
    private final TermStatistics[] termStats;
    private float score;    // score of the term
    private float idf;  // idf value of the term
    private float boost;    // boost value of the term
    private float avgdl;    // average document length
    private float k1;   // BM25 parameters
    private float b;    // BM25 parameters
    private String str = null;

    public SearchResult(CollectionStatistics collectionStats, TermStatistics... termStats) {
        this.collectionStats = new CollectionStatistics(collectionStats.field(), collectionStats.maxDoc(), collectionStats.docCount(), collectionStats.sumTotalTermFreq(), collectionStats.sumDocFreq());
        this.termStats = new TermStatistics[termStats.length];
        for (int i = 0; i < termStats.length; i++) {
            this.termStats[i] = new TermStatistics(termStats[i].term(), termStats[i].docFreq(), termStats[i].totalTermFreq());
        }
    }

    public String getTerm() {
        // return all terms in the termStats divided by space
        StringBuilder sb = new StringBuilder();
        for (TermStatistics termStat : termStats) {
            sb.append(Term.toString(termStat.term()));
            sb.append(" ");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public float computeScore(float freq, float dl) {
        float tf = (freq) / (freq + k1 * (1 - b + b * dl / avgdl));// Different with BM25 in Lucene
        return idf * tf * boost;
    }

    public float computeTF(float freq, float dl) {
        return (freq) / (freq + k1 * (1 - b + b * dl / avgdl));
    }

    public float getB() {
        return b;
    }

    public void setB(float b) {
        this.b = b;
    }

    public float getK1() {
        return k1;
    }

    public void setK1(float k1) {
        this.k1 = k1;
    }

    public float getAvgdl() {
        return avgdl;
    }

    public void setAvgdl(float avgdl) {
        this.avgdl = avgdl;
    }

    //  for debug
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nCollectionStats: ");
        sb.append(collectionStats.toString());
        sb.append("\nTermStats: ");
        for (TermStatistics termStat : termStats) {
            sb.append(termStat.toString());
        }
        sb.append("\nIDF: ");
        sb.append(idf);
        sb.append("\nBoost: ");
        sb.append(boost);
        sb.append("\nScore: ");
        sb.append(score);
        sb.append("\nk1: ");
        sb.append(k1);
        sb.append("\nb: ");
        sb.append(b);
        sb.append("\navgdl: ");
        sb.append(avgdl);
        return sb.toString();
    }

    // for key
    public String toStringCode() {
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(collectionStats.field());
        sb.append(":");
        for (TermStatistics termStat : termStats) {
            sb.append(Term.toString(termStat.term()));
        }
        str = sb.toString();
        return sb.toString();
    }

    public int hashCode() {
        return toStringCode().hashCode();
    }

    public CollectionStatistics getCollectionStats() {
        return collectionStats;
    }

    public TermStatistics[] getTermStats() {
        return termStats;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public float getIdf() {
        return idf;
    }

    public void setIdf(float idf) {
        this.idf = idf;
    }

    public float getBoost() {
        return boost;
    }

    public void setBoost(float boost) {
        this.boost = boost;
    }


}
