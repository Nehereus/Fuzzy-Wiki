package edu.uci.cs230.team10;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
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
    private final static Path mainIndexPath = Path.of("/home/hadoop/luceneIndex");
    //private final static Path mainIndexPath = Path.of("/Users/nickdu/Downloads/luceneIndex");
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
    private static final IndexSearcher searcher= new IndexSearcher(reader);

    /*a basic fuzzy searcher*/
    protected ScoreDoc[] search(String query) throws IOException, ParseException, QueryNodeException {
        //logger.info("Searching for: " + query);
        //the ts is never closed
        Analyzer analyzer = new StandardAnalyzer();
        String fuzzyQuery = query + "~";
        Query q = new StandardQueryParser(analyzer).parse(fuzzyQuery, "text");
        return searcher.search(q, 10).scoreDocs;
    }

    public static void main(String[] args) throws IOException, ParseException, QueryNodeException {
        Searcher searcher = new Searcher();
        ScoreDoc[] hits = searcher.search(args[0]);
        int i = 0;
        for (ScoreDoc hit : hits) {
            Document d = reader.storedFields().document(hit.doc);
            System.out.println(i+": " + d.get("title"));
        }
    }

}
