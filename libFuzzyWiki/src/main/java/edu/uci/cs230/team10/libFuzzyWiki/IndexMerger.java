package edu.uci.cs230.team10.libFuzzyWiki;
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
    private final static Path ROOT_DIRECTORY = Path.of("/home/hadoop/index");
    private final static Path mainIndexPath = Path.of("/home/hadoop/luceneIndex");

    public static void main(String[] args) throws IOException {
        try (Directory mainIndex = FSDirectory.open(mainIndexPath); DirectoryStream<Path> stream = Files.newDirectoryStream(ROOT_DIRECTORY, "*")) {
            IndexWriter writer = new IndexWriter(mainIndex, new IndexWriterConfig());
            for (Path subDir : stream) {
                logger.info("Merging index: " + subDir.toString());
                final Path lockFile = Path.of(subDir.toString(), "write.lock");
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
        }
    }
}
