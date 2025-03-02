package edu.uci.cs230.team10.libFuzzyWiki;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;

public class myTermStats {
    public static int getTermFrequency(IndexReader reader, String field, String termText, int docId) throws IOException {
        if (termText.split(("\\s+")).length > 1) {
            return getPhraseFrequency(reader, field, termText, docId);
        }
        LeafReaderContext leafContext = reader.leaves().get(ReaderUtil.subIndex(docId, reader.leaves()));
        LeafReader leafReader = leafContext.reader();
        int localDocId = docId - leafContext.docBase;
        Terms terms = leafReader.terms(field);
        if (terms == null) return 0;
        TermsEnum termsEnum = terms.iterator();
        if (termsEnum.seekExact(new BytesRef(termText))) {
            PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);
            if (postingsEnum != null && postingsEnum.advance(localDocId) == localDocId) {
                return postingsEnum.freq();
            }
        }
        return 0;
    }

    public static int getPhraseFrequency(IndexReader reader, String field, String phrase, int docId) throws IOException {
        IndexSearcher searcher = new IndexSearcher(reader);
        String[] phraseWords = phrase.split("\\s+");

        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        for (String word : phraseWords) {
            builder.add(new Term(field, word));
        }
        if (field.equals("text")) {
            builder.setSlop(3);
        } else {
            builder.setSlop(1);
        }
        PhraseQuery phraseQuery = builder.build();
        Weight weight = phraseQuery.createWeight(searcher, ScoreMode.COMPLETE_NO_SCORES, 1.0f);

        LeafReaderContext leafContext = reader.leaves().get(ReaderUtil.subIndex(docId, reader.leaves()));
        LeafReader leafReader = leafContext.reader();
        int localDocId = docId - leafContext.docBase;

        Scorer scorer = weight.scorer(leafContext);

        if (scorer != null && scorer.iterator().advance(localDocId) == localDocId) {
            Terms terms = leafReader.terms(field);
            if (terms == null) return 0;

            TermsEnum termsEnum = terms.iterator();
            int totalFreq = 0;
            for (String term : phrase.split("\\s+")) {
                if (termsEnum.seekExact(new BytesRef(term))) {
                    PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);
                    if (postingsEnum != null && postingsEnum.advance(localDocId) == localDocId) {
                        totalFreq += postingsEnum.freq();
                    }
                }
            }
            return totalFreq;
        }
        return 0;
    }

    public static int getDocumentLength(IndexReader reader, String field, int docId) throws IOException {
        LeafReaderContext leafContext = reader.leaves().get(ReaderUtil.subIndex(docId, reader.leaves()));
        LeafReader leafReader = leafContext.reader();
        int localDocId = docId - leafContext.docBase;

        Terms terms = leafReader.terms(field);
        if (terms == null) return 0;

        TermsEnum termsEnum = terms.iterator();
        PostingsEnum postingsEnum = null;
        int length = 0;

        while (termsEnum.next() != null) {
            postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.FREQS);
            if (postingsEnum != null && postingsEnum.advance(localDocId) == localDocId) {
                length += postingsEnum.freq();
            }
        }
        return length;
    }


}

