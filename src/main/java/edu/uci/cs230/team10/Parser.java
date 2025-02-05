package edu.uci.cs230.team10;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.json.JSONObject;

import java.util.List;
import java.util.logging.Logger;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class Parser extends Mapper<Object, Text, Text, Text> {
    private static final CharArraySet STOP_WORDS = new CharArraySet(Arrays.asList("r","title","text","short description","redirect","n","i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"), true);
    private Logger log = Logger.getLogger(Parser.class.getName());
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String json = value.toString();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("text") && jsonObject.has("title")) {
                Iterable<String> tokens = tokenize(jsonObject.get("text").toString());
                for (String token : tokens) {
                    context.write(new Text(jsonObject.getString("title")),new Text(token));
                }
            } else {
                context.getCounter("ParserErrors", "MissingFields").increment(1);
            }
        } catch (Exception e) {
            context.getCounter("ParserErrors", "JSONException").increment(1);
            log.warning("Error parsing JSON: " + json);
            e.printStackTrace();
        }
    }

    public Iterable<String> tokenize(String text) throws IOException {
        List<String> tokens = new ArrayList();
        StandardTokenizer tokenizer = new StandardTokenizer();
        tokenizer.setReader(new StringReader(text));
        TokenStream ts = new PorterStemFilter(
               new StopFilter(
                        new LowerCaseFilter(tokenizer),
                        STOP_WORDS)
        );
        CharTermAttribute charTermAttr = ts.addAttribute(CharTermAttribute.class);

        try {
            ts.reset();
            while (ts.incrementToken()) {
                String token = charTermAttr.toString();
                tokens.add(token);
                if (token.length()>32766) {
                    log.warning("Token length exceeds 32766: " + charTermAttr.toString());
                }
            }
        }catch (Exception e) {
            //e.printStackTrace();
            throw e;
        }
        finally {
            ts.close();
        }
        return tokens;
    }
}
