package edu.uci.cs230.team10;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;


class ParserTest {
    Logger log = Logger.getLogger(Parser.class.getName());

    @Test
    void testTokenize() throws IOException {
        // Path to the test data file
        String filePath = Paths.get("testData/top10.jsonl").toString();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            Parser parser = new Parser();

            while ((line = reader.readLine()) != null) {
                var res = parser.tokenize(line);
                log.info("Tokenized result: " + res);
                // Assert that the result is not null
                assertNotNull(res, "Tokenized result should not be null");

            }
        }
    }
}
