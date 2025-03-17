package edu.uci.cs230.team10.libFuzzyWiki;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public class Searcher {
    private final IndexReader reader;
    private final MyBM25Similarity mySimilarity;
    private final IndexSearcher iSearcher;
    Logger logger = Logger.getLogger(Searcher.class.getName());

    public void clearSearchResultMap() {
        mySimilarity.clearSearchResultMap();
    }

    public Searcher(Path mainIndexPath) {
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

    // function used to get the document by title without searching
    /**
     * @param title the title of the document
     * @return the document with the given title, or null if not found
     * @throws IOException
     */
    public MyScoredDoc getByTitle(String title) throws IOException, QueryNodeException {
        Query q = new StandardQueryParser(new StandardAnalyzer()).parse(title, "title");
        ScoreDoc[] hit = iSearcher.search(q, 1).scoreDocs;
        if(hit.length == 0) {
            return null;
        }else{
            Document d = reader.storedFields().document(hit[0].doc);
            if(!d.get("title").equals(title)) {
                logger.warning("Title " + title+ " not found, only found: "+ d.get("title"));
                return null;
            }

            return new MyScoredDoc(d.get("title"),0, d.get("originalText"));
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

        return new StandardQueryParser(analyzer).parse(finalQuery, "text");
    }

    public DocTermInfo searchForMerge(String query) throws QueryNodeException, IOException {
        return searchForMerge(query, 10);
    }

    public DocTermInfo searchForMerge(String query, int num) throws QueryNodeException, IOException {

        DocTermInfo docTermInfo = new DocTermInfo();
        Map<String, Map<String, float[]>> map = docTermInfo.infoMap;   // Document -> (field:term -> [boost*idf, tf])
        Map<String, Float> weightMap = docTermInfo.weightMap;   // field:term -> boost*idf
        Map<String, SearchResult> searchResultMap = mySimilarity.getSearchResultMap();
        Map<String, String> textMap = docTermInfo.textMap;   // Document -> text

        ScoreDoc[] hits = search(query, num);   // search for the query

        for (ScoreDoc hit : hits) {
            Document d = reader.storedFields().document(hit.doc);
            map.put(d.get("title"), new HashMap<>());
            for (SearchResult searchResult : searchResultMap.values()) {
                String field = searchResult.getCollectionStats().field();
                String term = searchResult.getTerm();
                String key = field + ":" + term;
                float[] info = new float[2];
                info[0] = searchResult.getBoost() * searchResult.getIdf();   // weight
                info[1] = searchResult.computeTF(myTermStats.getTermFrequency(this.reader, field, term, hit.doc)
                        , myTermStats.getDocumentLength(this.reader, field, hit.doc));   // tf
                map.get(d.get("title")).put(key, info);
            }
            textMap.put(d.get("title"), d.get("originalText"));
        }
        // get weight for each term
        for (SearchResult searchResult : searchResultMap.values()) {
            String field = searchResult.getCollectionStats().field();
            String term = searchResult.getTerm();
            String key = field + ":" + term;
            weightMap.put(key, searchResult.getBoost() * searchResult.getIdf());
        }

        return docTermInfo;
    }
public static void main(String[] args) throws IOException, QueryNodeException {
    Path mainIndexPath = System.getenv("INDEX_PATH") == null ? Path.of("./slave0/") : Path.of(System.getenv("INDEX_PATH"));
    Searcher searcher1 = new Searcher(mainIndexPath);
    DocTermInfo docTermInfo1 = searcher1.searchForMerge("Computer", 5);

    mainIndexPath = System.getenv("INDEX_PATH") == null ? Path.of("./slave1/") : Path.of(System.getenv("INDEX_PATH"));
    Searcher searcher2 = new Searcher(mainIndexPath);
    DocTermInfo docTermInfo2 = searcher2.searchForMerge("Computer", 5);
    List<DocTermInfo> docTermInfoList = new ArrayList<>();
    docTermInfoList.add(docTermInfo1);
    docTermInfoList.add(docTermInfo2);
    List<MyScoredDoc> ret = DocTermInfoHandler.mergeAndRank(docTermInfoList);

    System.out.println("----------After Merged----------");
    for (var sd : ret) {
        String docTitle = sd.title;
        float score = sd.score;
        System.out.println("DocTitle: " + docTitle + " |Score: " + score);
    }

    System.out.println("----------Before Merged----------");
    System.out.println("----------slave0----------");
    ScoreDoc[] hits = searcher1.search("Computer", 5);
    for (ScoreDoc hit : hits) {
        Document d = searcher1.reader.storedFields().document(hit.doc);
        System.out.println("Lucene: ID: " + hit.doc + ": " + d.get("title") + " |Score: " + hit.score);
    }
    System.out.println("----------slave1----------");
    hits = searcher2.search("Computer", 5);
    for (ScoreDoc hit : hits) {
        Document d = searcher2.reader.storedFields().document(hit.doc);
        System.out.println("Lucene: ID: " + hit.doc + ": " + d.get("title") + " |Score: " + hit.score);
    }
}

}
