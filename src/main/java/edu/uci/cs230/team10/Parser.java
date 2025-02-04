package edu.uci.cs230.team10;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.json.JSONObject;
import java.util.logging.Logger;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class Parser extends Mapper<Object, Text, Text, Text> {
    private static final CharArraySet STOP_WORDS = new CharArraySet(Arrays.asList("is", "an", "in", "the", "and", "a", "of"), true);
    private Logger log = Logger.getLogger(Parser.class.getName());
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String json = value.toString();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("text") && jsonObject.has("title")) {
                String text = tokenize(jsonObject.get("text").toString());
                context.write(new Text(jsonObject.getString("title")), new Text(text));
            } else {
                context.getCounter("ParserErrors", "MissingFields").increment(1);
            }
        } catch (Exception e) {
            context.getCounter("ParserErrors", "JSONException").increment(1);
            log.warning("Error parsing JSON: " + json);
            e.printStackTrace();
        }
    }

    public String tokenize(String text) throws IOException {
        StandardTokenizer tokenizer = new StandardTokenizer();
        tokenizer.setReader(new StringReader(text));

        TokenStream ts = new PorterStemFilter(
                new StopFilter(
                        new LowerCaseFilter(tokenizer),
                        STOP_WORDS)
        );
        StringBuilder sb = new StringBuilder();
        CharTermAttribute charTermAttr = ts.addAttribute(CharTermAttribute.class);

        try {
            ts.reset();
            while (ts.incrementToken()) {
                sb.append(charTermAttr.toString()).append(" ");
            }
            ts.end();
        } finally {
            ts.close();
        }
        return sb.toString().trim();
    }
}
