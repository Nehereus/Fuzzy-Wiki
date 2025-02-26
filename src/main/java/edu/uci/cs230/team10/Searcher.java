package edu.uci.cs230.team10;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.sound.midi.SysexMessage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Path;

public class Searcher {
    private final static Path mainIndexPath = System.getenv("INDEX_PATH") == null ? Path.of("./slave0/") : Path.of(System.getenv("INDEX_PATH"));
    public static final IndexReader reader;
    private static final Logger logger = Logger.getLogger(Searcher.class.getName());

    static {
        try {
            Directory directory = FSDirectory.open(mainIndexPath);
            reader = DirectoryReader.open(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


//    private static final MyBM25Similarity mySimilarity = new MyBM25Similarity();
    public static final MyBM25Similarity mySimilarity = new MyBM25Similarity();
    private static final IndexSearcher iSearcher = new IndexSearcher(reader);

    static {
        iSearcher.setSimilarity(mySimilarity);  // set my similarity for the searcher to get idf, tf, and boost
    }

    /*a basic fuzzy searcher*/
    protected static ScoreDoc[] search(String query) throws IOException,  QueryNodeException {
        //logger.info("Searching for: " + query);
        //the ts is never closed
        Query q = parseQuery(query);
        return iSearcher.search(q, 10).scoreDocs;
    }

    protected static Query parseQuery(String query) throws QueryNodeException {
        Analyzer analyzer = new StandardAnalyzer();
        final float titleBoost = 1.5f;
        final float textBoost = 1.2f;
        final float titleSplitBoost = 1.1f;
        final float textSplitBoost = 0.8f;

        final String titleQuery = String.format("title:\"%s\"~1", query);
        final String textQuery = String.format("text:\"%s\"~3", query);

        String finalQuery = String.format("%s^%s OR %s^%s", titleQuery, titleBoost, textQuery, textBoost);

        if (query.contains(" ")) {
            final String splitsQuery = String.format("title:(%s)^%s OR text:(%s)^%s", query, titleSplitBoost, query, textSplitBoost);
            finalQuery = String.format("%s OR %s", finalQuery, splitsQuery);
        }

//        logger.info("Final Query: " + finalQuery);
        System.out.println("Final Query: " + finalQuery);
        return new StandardQueryParser(analyzer).parse(finalQuery, "text");
    }

    public static String interpret(Query q,ScoreDoc[] hits) throws IOException {
        StringBuilder sb = new StringBuilder();
        int i=0;
        StoredFields storedFields = reader.storedFields();
        for (ScoreDoc hit : hits) {
            Document d = storedFields.document(hit.doc);
            sb.append(hit.doc + ": "+ d.get("title")+"\n reason: "+ iSearcher.explain(q,hit.doc)).append("\n").append("\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException, QueryNodeException {
//        ScoreDoc[] hits = Searcher.search(args[0]);
        MyBM25Similarity mySimilarity = Searcher.mySimilarity;
        ScoreDoc[] hits = Searcher.search("Computer Science and Technology");   // search for the query
//        System.out.println(mySimilarity.searchResultMap);
        for (int i=0;i<hits.length;i++) {
            ScoreDoc hit = hits[i];
            Document d = reader.storedFields().document(hit.doc);
//            System.out.println("Origin"+hit.doc+": " + d.get("title")+ " "+ hit.score);
        }
        Map<String, SearchResult> searchResultMap = mySimilarity.searchResultMap;
        Set<SearchResult> searchResults = new HashSet<>(searchResultMap.values());
        float freq[][] = new float[hits.length][searchResultMap.size()];
        float dl[][] = new float[hits.length][searchResultMap.size()];
        float score[][] = new float[hits.length][searchResultMap.size()];
        for(int i=0;i<hits.length;i++){
            ScoreDoc hit = hits[i];
            int j = 0;
            for(SearchResult searchResult: searchResults){
                String field = searchResult.getCollectionStats().field();
                String term = searchResult.getTerm();
                freq[i][j] = LuceneTermStats.getTermFrequency(reader, field, term, hit.doc);
                dl[i][j] = LuceneTermStats.getDocumentLength(reader, field, hit.doc);
//                System.out.println("field:"+field+" term:"+term+" freq:"+freq[i][j]);
                score[i][j] = searchResult.computeScore(freq[i][j], dl[i][j]);
//                System.out.println(freq[i][j]+" "+dl[i][j]+" "+score[i][j]);
                j++;
            }
        }
//        System.out.println("SearchResults:"+searchResults);
        for(int i=0;i<hits.length;i++){
            ScoreDoc hit = hits[i];
            Document d = reader.storedFields().document(hit.doc);
            System.out.println("Lucene: ID: "+hit.doc+": " + d.get("title")+ " Score: "+ hit.score);
            System.out.print("Mine: ID: "+hit.doc+": " + d.get("title")+ " ");
            float sum = 0;
            for(int j=0;j<searchResults.size();j++){
                sum += score[i][j];
//                System.out.print(score[i][j]+" ");
//                System.out.println((searchResults.toArray())[j]);
            }
            System.out.println("score:"+sum);
        }
//        System.out.println(interpret(parseQuery("Auckland Zoo"), hits));

    }

    //private method used for testing if there are duplicate items in the index;
    private static void detectDupDocs(String str) throws IOException {
        for (int j = 0; j < reader.maxDoc(); j++) {
            Document d = reader.storedFields().document(j);
            if (d.get("title").contains(str)) {
                System.out.println(j  + ": " + d.get("title"));
            }
        }
    }

}
