package edu.uci.cs230.team10;

import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import java.io.IOException;
import java.nio.file.Paths;

public class LuceneTermStats {
    public static int getTermFrequency(IndexReader reader, String field, String termText, int docId) throws IOException {
        LeafReaderContext leafContext = reader.leaves().get(ReaderUtil.subIndex(docId, reader.leaves()));
        LeafReader leafReader = leafContext.reader();
        int localDocId = docId - leafContext.docBase; // 转换为 segment 内部 docId

        Terms terms = leafReader.terms(field);
        if (terms == null) return 0;

        TermsEnum termsEnum = terms.iterator();
        if (termsEnum.seekExact(new BytesRef(termText))) { // 定位到 term
            PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);
            if (postingsEnum != null && postingsEnum.advance(localDocId) == localDocId) {
                return postingsEnum.freq(); // 返回词频
            }
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
                length += postingsEnum.freq(); // 累加当前 doc 中所有 term 的词频
            }
        }
        return length;
    }



}

