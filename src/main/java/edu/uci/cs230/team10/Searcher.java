package edu.uci.cs230.team10;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Path;

public class Searcher {
    private final static Path mainIndexPath = Path.of("/home/hadoop/luceneIndex");
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
    protected ScoreDoc[] search(String query) throws IOException {
        logger.info("Searching for: " + query);
        //the ts is never closed
        FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term("text", Tokenizer.tokenize(query).toString()));
        return searcher.search(fuzzyQuery, 10).scoreDocs;
    }

    public static void main(String[] args) throws IOException {
        Searcher searcher = new Searcher();
        ScoreDoc[] hits = searcher.search(args[0]);
        int i = 0;
        for (ScoreDoc hit : hits) {
            Document d = reader.storedFields().document(hit.doc);
            System.out.println(i+": " + d.get("title"));
        }
    }

}
