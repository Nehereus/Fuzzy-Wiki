package edu.uci.cs230.team10;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.json.JSONObject;

import java.util.List;
import java.util.logging.Logger;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class Parser extends Mapper<Object, Text, Text, Text> {
    private final Logger log = Logger.getLogger(Parser.class.getName());
    private static final int MAX_TOKEN_LENGTH = 32766;

    @Override
    public void map(Object key, Text value, Context context){
        String json = value.toString();
        try {
            JSONObject jsonObject = new JSONObject(json);
            String title;
            if (jsonObject.has("text") && jsonObject.has("title")) {
                title = jsonObject.getString("title");
                Iterable<String> tokens = tokenize(jsonObject.get("text").toString());

                for (String token : tokens) {
                    context.write(new Text(title), new Text(token));
                }
            } else {
                context.getCounter("ParserErrors", "MissingFields").increment(1);
            }
        } catch (Exception e) {
            context.getCounter("ParserErrors", "JSONException").increment(1);
            e.printStackTrace();
        }
    }

    public Iterable<String> tokenize(String text) throws IOException {
        List<String> tokens = new ArrayList<>();
        TokenStream ts = Tokenizer.tokenize(text);

        try (ts) {
            CharTermAttribute charTermAttr = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            StringBuilder sb = new StringBuilder();
            int sbLength = 0;
            while (ts.incrementToken()) {
                String token = charTermAttr.toString();
                int tokenLength = token.getBytes(StandardCharsets.UTF_8).length;
                // +1 for the space
                if (sbLength +tokenLength  + 1 > MAX_TOKEN_LENGTH) {
                    tokens.add(sb.toString());
                    sb = new StringBuilder();
                    sbLength = 0;
                }
                sbLength += tokenLength + 1;
                sb.append(token).append(" ");
            }
        } catch (Exception e) {
            throw e;
        }
        return tokens;
    }
}
