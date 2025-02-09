package edu.uci.cs230.team10;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Path;

public class Searcher {
    private static Path mainIndexPath = System.getenv("INDEX_PATH") == null ? Path.of("/home/hadoop/luceneIndex") : Path.of(System.getenv("INDEX_PATH"));
    private static final IndexReader reader;
    private static final Logger logger = Logger.getLogger(Searcher.class.getName());

    static {
        try {
            Directory directory = FSDirectory.open(mainIndexPath);
            reader = DirectoryReader.open(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final IndexSearcher iSearcher = new IndexSearcher(reader);

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
        final float textBoost = 0.7f;
        final String titleQuery = String.format("title:\"%s\"", query);
        final String textQuery = String.format("text:\"%s~\"", query);
        final String finalQuery = String.format("%s^%s OR %s^%s", titleQuery, titleBoost, textQuery, textBoost);
        logger.info("Final Query: " + finalQuery);
        return new StandardQueryParser(analyzer).parse(finalQuery, "text");
    }

    public static String interpret(Query q,ScoreDoc[] hits) throws IOException {
        StringBuilder sb = new StringBuilder();
        int i=0;
        StoredFields storedFields = reader. storedFields();
        for (ScoreDoc hit : hits) {
            Document d = storedFields.document(hit.doc);
            sb.append(i++ + ": "+ d.get("text")+"\n reason: "+ iSearcher.explain(q,hit.doc)).append("\n").append("\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException, ParseException, QueryNodeException {
        ScoreDoc[] hits = Searcher.search(args[0]);

        int i = 0;
        for (ScoreDoc hit : hits) {
            Document d = reader.storedFields().document(hit.doc);
            System.out.println(i+": " + d.get("title"));
        }
    }

}
