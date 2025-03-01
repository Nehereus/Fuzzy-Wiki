package edu.uci.cs230.team10.libFuzzyWiki;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class DocTermInfoTest {
    private static final Logger logger = Logger.getLogger(DocTermInfoTest.class.getName());
    static DocTermInfo testDti;
    static JSONObject testJson = new JSONObject();

    @BeforeAll
    static void beforeAll() {
        Map<String, Map<String, float[]>> infoMap = new HashMap<>();
        Map<String, float[]> exEntry = new HashMap<>();
        exEntry.put("term0", new float[]{1.2f, 2.3f});
        infoMap.put("doc0", exEntry);
        Map<String, Float> weightMap = new HashMap<>();
        weightMap.put("term0", 1.7f);

        testDti = new DocTermInfo(infoMap, weightMap, new HashMap<>());
    }

    @Test
    void toJson() {
        testJson = testDti.toJson();
    }

    //infoMap and weightMap does not have a equals method implemented, test using toString for now
    @Test
    void from() {
        DocTermInfo recreatedDti = DocTermInfo.from(testJson);
        logger.info("recreatedDti: " + recreatedDti.toString());
    }


}
