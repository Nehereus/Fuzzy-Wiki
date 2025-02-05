package edu.uci.cs230.team10;

import org.apache.hadoop.io.NullWritable;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

public class Indexer extends Reducer<Text, Text, NullWritable, NullWritable> {
    private static final Random random = new Random();
    private static final String ROOT_DIRECTORY = "/home/hadoop/index";
    private String selectedDir;
    private  IndexWriter writer;
    private  StandardAnalyzer analyzer;
    private  Directory index;

    protected void setWriter(IndexWriter writer) {
        this.writer = writer;
    }
    private void chooseIndexer() throws IOException {
        Path queueFilePath = Path.of(ROOT_DIRECTORY, "index_queue.txt");
        if (!Files.exists(queueFilePath)) {
            Files.createFile(queueFilePath); // Create the queue file if it doesn't exist
        }

        String selectedDir = null;

        // Check file size first to avoid unnecessary reads
        if (Files.size(queueFilePath) > 0) {
            try (BufferedReader reader = Files.newBufferedReader(queueFilePath)) {
                selectedDir = reader.readLine(); // Read only the first line
            }

            if (selectedDir != null) {
                // Remove the first line and rewrite the rest of the file
                List<String> remainingLines = Files.readAllLines(queueFilePath);
                remainingLines.remove(0);
                Files.write(queueFilePath, remainingLines);
            }
        }

        if (selectedDir == null) {
            // Create a new directory if the queue is empty
            selectedDir = ROOT_DIRECTORY + "/index-" + System.currentTimeMillis() + "-" + random.nextInt();
            Files.createDirectories(Path.of(selectedDir));
        }

        // Initialize index writer
        index = FSDirectory.open(Path.of(selectedDir));
        analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(index, config);
        this.selectedDir = selectedDir;
    }

    @Override
    protected void setup(Context context) throws IOException {
        chooseIndexer();
    }

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", key.toString(), Field.Store.YES));

        for (Text value : values) {
            doc.add(new StringField("text", value.toString(), Field.Store.YES));
        }

        if(writer.isOpen()){
            writer.addDocument(doc);
        }else{
            context.getCounter("IndexerErrors", "LockObtainFailedException").increment(1);
            chooseIndexer();
            reduce(key, values, context);
        }

        writer.commit();
    }

    @Override
    protected void cleanup(Context context) throws IOException {
        analyzer.close();
        writer.commit();
        writer.close();
        index.close();
        Path queueFilePath = Path.of(ROOT_DIRECTORY, "index_queue.txt");
        List<String> lines = Files.readAllLines(queueFilePath);
        lines.add(selectedDir); // Re-add the directory to the end of the queue
        Files.write(queueFilePath, lines);
    }
}
