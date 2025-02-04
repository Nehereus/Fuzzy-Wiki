package edu.uci.cs230.team10;

import java.io.IOException;

class ParserTest {

    @org.junit.jupiter.api.Test
    void tokenize() throws IOException {
        Parser parser = new Parser();
        parser.tokenize("WhaT your name test");
    }
}
