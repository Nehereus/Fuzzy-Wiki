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
    private final static Path mainIndexPath = Path.of(ROOT_DIRECTORY);

    public static void main(String[] args) throws IOException {
        Directory mainIndex = FSDirectory.open(mainIndexPath);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(ROOT_DIRECTORY), "index-*")) {

            IndexWriter writer = new IndexWriter(mainIndex, new IndexWriterConfig());

            for (Path subDir : stream) {
                final Path lockFile= Path.of(subDir.toString(), "write.lock");
                //remove write lock if it exists, assuming all updating has been done at this stage
                if (Files.exists(lockFile))
                    Files.delete(lockFile);

                Directory subIndex = FSDirectory.open(subDir);

                //open an index writer for the sub index to close the unclosed index;
                writer.addIndexes(subIndex);
                writer.commit();
            }
        } catch (IOException e) {
            logger.severe("Error merging indexes: " + e.getMessage());
        }finally {
            mainIndex.close();
        }
    }
}
