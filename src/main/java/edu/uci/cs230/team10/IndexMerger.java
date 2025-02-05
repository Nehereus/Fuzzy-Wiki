package edu.uci.cs230.team10;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class IndexMerger {
    private final static Logger logger = Logger.getLogger(IndexMerger.class.getName());
    private static final String ROOT_DIRECTORY = "/home/hadoop/index";
    private final static Path mainIndexPath = Path.of(ROOT_DIRECTORY, "mainIndex");

    public static void main(String[] args) throws IOException {
        Directory mainIndex = FSDirectory.open(mainIndexPath);

        try (IndexWriter writer = new IndexWriter(mainIndex, new IndexWriterConfig());
             DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(ROOT_DIRECTORY), "index-*")) {
            for (Path subDir : stream) {
                Directory subIndex = FSDirectory.open(subDir);
                writer.addIndexes(subIndex);
            }
        } catch (IOException e) {
            logger.warning("Error reading index directories: " + e.getMessage());
        }
    }
}
