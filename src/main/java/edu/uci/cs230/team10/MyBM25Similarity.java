//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package edu.uci.cs230.team10;

import java.util.*;

import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.SmallFloat;

public class MyBM25Similarity extends Similarity {
    private final float k1;
    private final float b;
    private static final float[] LENGTH_TABLE = new float[256];
    public final Map<String, SearchResult> searchResultMap = new Hashtable<>();

    public Map<String, SearchResult> getSearchResultMap(){
        return searchResultMap;
    }

    public void clearSearchResultMap(){
        searchResultMap.clear();
    }

    public MyBM25Similarity(float k1, float b, boolean discountOverlaps) {
        super(discountOverlaps);
        if (Float.isFinite(k1) && !(k1 < 0.0F)) {
            if (!Float.isNaN(b) && !(b < 0.0F) && !(b > 1.0F)) {
                this.k1 = k1;
                this.b = b;
            } else {
                throw new IllegalArgumentException("illegal b value: " + b + ", must be between 0 and 1");
            }
        } else {
            throw new IllegalArgumentException("illegal k1 value: " + k1 + ", must be a non-negative finite value");
        }
    }

    public MyBM25Similarity(float k1, float b) {
        this(k1, b, true);
    }

    public MyBM25Similarity(boolean discountOverlaps) {
        this(1.2F, 0.75F, discountOverlaps);
    }

    public MyBM25Similarity() {
        this(1.2F, 0.75F, true);
    }

    protected float idf(long docFreq, long docCount) {
        return (float)Math.log(1.0 + ((double)(docCount - docFreq) + 0.5) / ((double)docFreq + 0.5));
    }

    protected float avgFieldLength(CollectionStatistics collectionStats) {
        return (float)((double)collectionStats.sumTotalTermFreq() / (double)collectionStats.docCount());
    }

    public Explanation idfExplain(CollectionStatistics collectionStats, TermStatistics termStats) {
        long df = termStats.docFreq();
        long docCount = collectionStats.docCount();
        float idf = this.idf(df, docCount);
        return Explanation.match(idf, "idf, computed as log(1 + (N - n + 0.5) / (n + 0.5)) from:", new Explanation[]{Explanation.match(df, "n, number of documents containing term", new Explanation[0]), Explanation.match(docCount, "N, total number of documents with field", new Explanation[0])});
    }

    public Explanation idfExplain(CollectionStatistics collectionStats, TermStatistics[] termStats) {
        double idf = 0.0;
        List<Explanation> details = new ArrayList();
        TermStatistics[] var6 = termStats;
        int var7 = termStats.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            TermStatistics stat = var6[var8];
            Explanation idfExplain = this.idfExplain(collectionStats, stat);
            details.add(idfExplain);
            idf += (double)idfExplain.getValue().floatValue();
        }

        return Explanation.match((float)idf, "idf, sum of:", details);
    }

    public final Similarity.SimScorer scorer(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
        Explanation idf = termStats.length == 1 ? this.idfExplain(collectionStats, termStats[0]) : this.idfExplain(collectionStats, termStats);
//
//        System.out.println("CollectionStats: " + collectionStats);
//        System.out.println("TermStats: " + Arrays.toString(termStats));
//        System.out.println("IDF: " + idf+"\n");
        float avgdl = this.avgFieldLength(collectionStats);
        float[] cache = new float[256];

        for(int i = 0; i < cache.length; ++i) {
            cache[i] = 1.0F / (this.k1 * (1.0F - this.b + this.b * LENGTH_TABLE[i] / avgdl));
        }
        SearchResult searchResult = new SearchResult(collectionStats, termStats);
        if(searchResultMap.containsKey(searchResult.toString())){
            return new BM25Scorer(boost, this.k1, this.b, idf, avgdl, cache);
        }
        searchResult.setIdf(idf.getValue().floatValue());
        searchResult.setBoost(boost);
        searchResult.setAvgdl(avgdl);
        searchResult.setK1(this.k1);
        searchResult.setB(this.b);
        searchResultMap.put(searchResult.toStringCode(), searchResult);
        return new BM25Scorer(boost, this.k1, this.b, idf, avgdl, cache);
    }

    public String toString() {
        return "BM25(k1=" + this.k1 + ",b=" + this.b + ")";
    }

    public final float getK1() {
        return this.k1;
    }

    public final float getB() {
        return this.b;
    }

    static {
        for(int i = 0; i < 256; ++i) {
            LENGTH_TABLE[i] = (float)SmallFloat.byte4ToInt((byte)i);
        }

    }

    private static class BM25Scorer extends Similarity.SimScorer {
        private final float boost;
        private final float k1;
        private final float b;
        private final Explanation idf;
        private final float avgdl;
        private final float[] cache;
        private final float weight;

        BM25Scorer(float boost, float k1, float b, Explanation idf, float avgdl, float[] cache) {
            this.boost = boost;
            this.idf = idf;
            this.avgdl = avgdl;
            this.k1 = k1;
            this.b = b;
            this.cache = cache;
            this.weight = boost * idf.getValue().floatValue();
        }

        public float score(float freq, long encodedNorm) {
            float normInverse = this.cache[(byte)((int)encodedNorm) & 255];
            return this.weight - this.weight / (1.0F + freq * normInverse);
        }

        public Explanation explain(Explanation freq, long encodedNorm) {
            List<Explanation> subs = new ArrayList(this.explainConstantFactors());
            Explanation tfExpl = this.explainTF(freq, encodedNorm);
            subs.add(tfExpl);
            float normInverse = this.cache[(byte)((int)encodedNorm) & 255];
            return Explanation.match(this.weight - this.weight / (1.0F + freq.getValue().floatValue() * normInverse), "score(freq=" + freq.getValue() + "), computed as boost * idf * tf from:", subs);
        }

        private Explanation explainTF(Explanation freq, long norm) {
            List<Explanation> subs = new ArrayList();
            subs.add(freq);
            subs.add(Explanation.match(this.k1, "k1, term saturation parameter", new Explanation[0]));
            float doclen = MyBM25Similarity.LENGTH_TABLE[(byte)((int)norm) & 255];
            subs.add(Explanation.match(this.b, "b, length normalization parameter", new Explanation[0]));
            if ((norm & 255L) > 39L) {
                subs.add(Explanation.match(doclen, "dl, length of field (approximate)", new Explanation[0]));
            } else {
                subs.add(Explanation.match(doclen, "dl, length of field", new Explanation[0]));
            }

            subs.add(Explanation.match(this.avgdl, "avgdl, average length of field", new Explanation[0]));
            float normInverse = 1.0F / (this.k1 * (1.0F - this.b + this.b * doclen / this.avgdl));
            return Explanation.match(1.0F - 1.0F / (1.0F + freq.getValue().floatValue() * normInverse), "tf, computed as freq / (freq + k1 * (1 - b + b * dl / avgdl)) from:", subs);
        }

        private List<Explanation> explainConstantFactors() {
            List<Explanation> subs = new ArrayList();
            if (this.boost != 1.0F) {
                subs.add(Explanation.match(this.boost, "boost", new Explanation[0]));
            }

            subs.add(this.idf);
            return subs;
        }
    }
}
