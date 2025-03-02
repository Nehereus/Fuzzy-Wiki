package edu.uci.cs230.team10.libFuzzyWiki;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.icu.ICUNormalizer2Filter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.StringReader;

public class Tokenizer {


    public static TokenStream tokenize(String text) {
        StandardTokenizer tokenizer = new StandardTokenizer();
        tokenizer.setReader(new StringReader(text));
        return new PorterStemFilter(
                new StopFilter(
                        new LowerCaseFilter(
                                new ICUNormalizer2Filter(tokenizer)),
                        EnglishAnalyzer.ENGLISH_STOP_WORDS_SET)
        );
    }
}
