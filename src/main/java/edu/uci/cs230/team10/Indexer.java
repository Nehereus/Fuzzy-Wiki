package edu.uci.cs230.team10;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
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
        doc.add(new TextField("title", key.toString(), Field.Store.YES));

        for (Text value : values) {
            doc.add(new StringField("text", value.toString(), Field.Store.YES));

            try {
                this.indexWriter.addDocument(doc);
            } catch (LockObtainFailedException e){
                context.getCounter("IndexerErrors", "addingDocumentLockObtainFailedException").increment(1);
            } catch (IOException e) {
                context.getCounter("IndexerErrors", "addingDocumentIOException").increment(1);
                e.printStackTrace();
            }
        }

    }

    @Override

    protected void cleanup(Context context) throws IOException, InterruptedException {
        indexWriter.close();
        // write the index directory to the context
        String indexDir = ROOT_DIRECTORY + context.getTaskAttemptID().getTaskID().getId();
        context.write(new Text("index"), new Text(indexDir));
    }
}
