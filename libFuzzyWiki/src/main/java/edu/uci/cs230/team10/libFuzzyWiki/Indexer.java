package edu.uci.cs230.team10.libFuzzyWiki;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import java.io.File;
import java.io.IOException;

public class Indexer extends Reducer<Text, Text, Text, Text> {
    private static final String ROOT_DIRECTORY = "/home/hadoop/index/";
    private IndexWriter indexWriter;
    private final static String ORIGIN_TOKEN = "@origin@";

    private void chooseIndexer(Context context) throws IOException {
        File indexDir = new File(ROOT_DIRECTORY + context.getTaskAttemptID().getTaskID().getId());
        Directory directory = FSDirectory.open(indexDir.toPath()); // open Lucene dir
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer()); // config IndexWriter
        this.indexWriter = new IndexWriter(directory, config); // init IndexWriter
    }

    @Override
    protected void setup(Context context) throws IOException {
        chooseIndexer(context);
    }

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException {
        Document doc = new Document();
        // I decide to use the text field for both fields cuz lucene may provide better search results with the text field optimization
        doc.add(new TextField("title", key.toString(), Field.Store.YES));

        for (Text value : values) {
            if(isOriginText(value)) {
                doc.add(new StoredField("originalText", value.toString().substring(ORIGIN_TOKEN.length())));
            }else {
                doc.add(new TextField("text", value.toString(), Field.Store.NO));
            }
        }


        try {
            this.indexWriter.addDocument(doc);
        } catch (LockObtainFailedException e) {
            context.getCounter("IndexerErrors", "addingDocumentLockObtainFailedException").increment(1);
        } catch (IOException e) {
            context.getCounter("IndexerErrors", "addingDocumentIOException").increment(1);
            throw e;
        }
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        indexWriter.close();
        // write the index directory to the context
        String indexDir = ROOT_DIRECTORY + context.getTaskAttemptID().getTaskID().getId();
        context.write(new Text("index"), new Text(indexDir));
    }

    /** a utility function used to determine if the text is the original text by identifying the special token */
    private static boolean isOriginText (Text text) {
        int len = ORIGIN_TOKEN.length();
        for(int i = 0; i < len; i++) {
            if (text.charAt(i) != ORIGIN_TOKEN.charAt(i)) {
                return false;
            }
        }
        return true;
    }


}
