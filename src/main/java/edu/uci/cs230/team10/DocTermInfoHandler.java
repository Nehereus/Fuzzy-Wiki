package edu.uci.cs230.team10;

import org.apache.lucene.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocTermInfoHandler {

    /**
     * Merge and rank the documents based on the average IDF*boost for each term;
     * @return List<Object[]> [Document: doc, float: score]
     */
    public static List<Object[]> mergeAndRank(List<DocTermInfo> docTermInfoList){

        List<Object[]> ret = new ArrayList<>(); // return the ranked documents [Document: doc, float: score]
        // step 1: avg IDF*boost for each term;
        DocTermInfo mergedDocTermInfo = docTermInfoList.get(0);
        for(int i = 1; i < docTermInfoList.size(); i++){
            DocTermInfo docTermInfo = docTermInfoList.get(i);
            for(Map.Entry<String, Float> entry: docTermInfo.weightMap.entrySet()){
                String term = entry.getKey();
                float avgIDF = entry.getValue();
                if(mergedDocTermInfo.weightMap.containsKey(term)){
                    mergedDocTermInfo.weightMap.compute(term, (k, sumIDF) -> sumIDF + avgIDF);
                }else{
                    mergedDocTermInfo.weightMap.put(term, avgIDF);
                }
            }
        }
        mergedDocTermInfo.weightMap.replaceAll((k, v) -> v / docTermInfoList.size());

        // step 2: compute the score for each document and fill the score into the return list;
        for (DocTermInfo docTermInfo : docTermInfoList) {
            for (Map.Entry<Document, Map<String, float[]>> entry : docTermInfo.infoMap.entrySet()) {
                Document doc = entry.getKey();
                float score = 0;
                for (Map.Entry<String, float[]> entry1 : entry.getValue().entrySet()) {
                    String term = entry1.getKey();
                    float[] values = entry1.getValue();
                    float IDF = mergedDocTermInfo.weightMap.get(term);
                    float TF = values[1];
                    score += IDF * TF;
                }
                ret.add(new Object[]{doc, score});
            }
        }

        // step 3: sort the return list by score;
        ret.sort((o1, o2) -> ((float) o2[1]-(float) o1[1])>0?1:-1);

        // step 4: return the top 10 documents;
        return ret;
    }
}
