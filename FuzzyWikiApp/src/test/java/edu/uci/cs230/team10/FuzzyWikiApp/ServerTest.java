package edu.uci.cs230.team10.FuzzyWikiApp;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

class ServerTest {

    @org.junit.jupiter.api.Test
    void documentHandler() {
    }

    @org.junit.jupiter.api.Test
    void searchHandler() {
    }
    @org.junit.jupiter.api.Test
    void tsetParseConfig() throws UnsupportedEncodingException {
        String query = "tom jerry";
        System.out.println( URLEncoder.encode(query, StandardCharsets.UTF_8));

    }

}
