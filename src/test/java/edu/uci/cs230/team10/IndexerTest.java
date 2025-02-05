package edu.uci.cs230.team10;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.jline.utils.Log;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.io.*;
import java.nio.file.Path;



import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexerTest {
    private static int MAX_TERM_LENGTH = 32766;
    Directory index;
    private IndexWriter writer;
    Parser parser;
    Indexer indexer;

    @BeforeEach
    void setup() throws IOException {
        index = new ByteBuffersDirectory();
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(index, config);
        parser = new Parser();
        indexer = new Indexer();
        indexer.setWriter(writer);
    }

    @Test
    void testReduce() throws IOException {
        String filePath = Paths.get("testData/top10.jsonl").toString();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject jsonObject = new JSONObject(line);
                Iterable<String> testInput = parser.tokenize(jsonObject.getString("text"));
                for (String token : testInput) {
                    try {

                        indexer.reduce(new Text(jsonObject.getString("title")),
                                (Iterable<Text>) Collections.singletonList(new Text(token)), null);
                    } catch (IllegalArgumentException e) {
                        Log.warn(e.getMessage());
                    }
                }
            }
            }
            }


    @Test
    void testCleanup() {
    }
    }

