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
import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.similarities.BM25Similarity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class Searcher {
    private final Path mainIndexPath;
    private final IndexReader reader;
    private final Logger logger = Logger.getLogger(Searcher.class.getName());
    private final MyBM25Similarity mySimilarity;
    private final IndexSearcher iSearcher;

    public Map<String, SearchResult> getSearchResultMap(){
        return mySimilarity.getSearchResultMap();
    }

    public void clearSearchResultMap(){
        mySimilarity.clearSearchResultMap();
    }

    public Searcher(Path mainIndexPath) {
        this.mainIndexPath = mainIndexPath;
        try {
            Directory directory = FSDirectory.open(mainIndexPath);
            this.reader = DirectoryReader.open(directory);
            this.mySimilarity = new MyBM25Similarity();
            this.iSearcher = new IndexSearcher(reader);
            this.iSearcher.setSimilarity(mySimilarity);  // set my similarity for the searcher to get idf, tf, and boost
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ScoreDoc[] search(String query) throws IOException, QueryNodeException {
        return search(query, 10);// default number of hits is 10
    }

    /*a basic fuzzy searcher*/
    public ScoreDoc[] search(String query, int num) throws IOException, QueryNodeException {
        clearSearchResultMap();
        Query q = parseQuery(query);
        return iSearcher.search(q, num).scoreDocs;
    }

    protected Query parseQuery(String query) throws QueryNodeException {
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
//        System.out.println("Final Query: " + finalQuery);
        return new StandardQueryParser(analyzer).parse(finalQuery, "text");
    }

    public String interpret(Query q, ScoreDoc[] hits) throws IOException {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        StoredFields storedFields = reader.storedFields();
        for (ScoreDoc hit : hits) {
            Document d = storedFields.document(hit.doc);
            sb.append(hit.doc + ": " + d.get("title") + "\n reason: " + iSearcher.explain(q, hit.doc)).append("\n").append("\n");
        }
        return sb.toString();
    }

    public DocTermInfo searchForMerge(String query, int num) throws QueryNodeException, IOException {

        DocTermInfo docTermInfo = new DocTermInfo();
        Map<Document, Map<String, float[]>> map = docTermInfo.infoMap;   // Document -> (field:term -> [boost*idf, tf])
        Map<String, Float> weightMap = docTermInfo.weightMap;   // field:term -> boost*idf
        Map<String, SearchResult> searchResultMap = this.getSearchResultMap();

        ScoreDoc[] hits = this.search(query, num);   // search for the query

        for (ScoreDoc hit : hits) {
            Document d = this.reader.storedFields().document(hit.doc);
            map.put(d, new HashMap<>());
//            System.out.println("Title: " + d.get("title") + " Score: " + hit.score);
            for (SearchResult searchResult : searchResultMap.values()) {
                String field = searchResult.getCollectionStats().field();
                String term = searchResult.getTerm();
                String key = field + ":" + term;
                float[] info = new float[2];
                info[0] = searchResult.getBoost()*searchResult.getIdf();   // weight
                info[1] = searchResult.computeTF(LuceneTermStats.getTermFrequency(this.reader, field, term, hit.doc)
                        ,LuceneTermStats.getDocumentLength(this.reader, field, hit.doc));   // tf
                map.get(d).put(key, info);
            }
        }
        // get weight for each term
        for(SearchResult searchResult: searchResultMap.values()){
            String field = searchResult.getCollectionStats().field();
            String term = searchResult.getTerm();
            String key = field + ":" + term;
            weightMap.put(key, searchResult.getBoost()*searchResult.getIdf());
        }

        return docTermInfo;
    }

    public static void main(String[] args) throws IOException, QueryNodeException {
        Path mainIndexPath = System.getenv("INDEX_PATH") == null ? Path.of("./slave0/") : Path.of(System.getenv("INDEX_PATH"));
        Searcher searcher1 = new Searcher(mainIndexPath);
        DocTermInfo docTermInfo1 = searcher1.searchForMerge("Computer", 5);
//        System.out.println(docTermInfo1.toString());

        mainIndexPath = System.getenv("INDEX_PATH") == null ? Path.of("./slave1/") : Path.of(System.getenv("INDEX_PATH"));
        Searcher searcher2 = new Searcher(mainIndexPath);
        DocTermInfo docTermInfo2 = searcher2.searchForMerge("Computer", 5);
//        System.out.println(docTermInfo2.toString());
        List<DocTermInfo> docTermInfoList = new ArrayList<>();
        docTermInfoList.add(docTermInfo1);
        docTermInfoList.add(docTermInfo2);
        List<Object[]> ret = DocTermInfoHandler.mergeAndRank(docTermInfoList);

        System.out.println("----------After Merged----------");
        for(Object[] obj: ret){
            Document doc = (Document) obj[0];
            float score = (float) obj[1];
            System.out.println("DocTitle: " + doc.get("title") + " |Score: " + score);
        }

        System.out.println("----------Before Merged----------");
        System.out.println("----------slave0----------");
        ScoreDoc[] hits = searcher1.search("Computer", 5);
        for (int i = 0; i < hits.length; i++) {
            ScoreDoc hit = hits[i];
            Document d = searcher1.reader.storedFields().document(hit.doc);
            System.out.println("Lucene: ID: " + hit.doc + ": " + d.get("title") + " |Score: " + hit.score);
        }
        System.out.println("----------slave1----------");
        hits = searcher2.search("Computer", 5);
        for (int i = 0; i < hits.length; i++) {
            ScoreDoc hit = hits[i];
            Document d = searcher2.reader.storedFields().document(hit.doc);
            System.out.println("Lucene: ID: " + hit.doc + ": " + d.get("title") + " |Score: " + hit.score);
        }
//        System.out.println(searcher1.interpret(searcher1.parseQuery("Computer Science"), hits));
    }

    //private method used for testing if there are duplicate items in the index;
    private void detectDupDocs(String str) throws IOException {
        for (int j = 0; j < reader.maxDoc(); j++) {
            Document d = reader.storedFields().document(j);
            if (d.get("title").contains(str)) {
                System.out.println(j + ": " + d.get("title"));
            }
        }
    }
}