package edu.uci.cs230.team10;

import org.apache.lucene.document.Document;

import java.util.HashMap;
import java.util.Map;

public class DocTermInfo {
    public Map<Document, Map<String, float[]>> infoMap; // Map<Doc, Map<Term, [IDF*boost, TF]>>;
    public Map<String, Float> weightMap; // average of IDF*boost for each term;

    public DocTermInfo(){
        infoMap = new HashMap<>();
        weightMap = new HashMap<>();
    }
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, Float> entry: weightMap.entrySet()){
            sb.append("Term: ");
            sb.append(entry.getKey());
            sb.append(" ");
            sb.append("AvgIDF*boost: ");
            sb.append(entry.getValue());
            sb.append("\n");
        }
        for(Map.Entry<Document, Map<String, float[]>> entry: infoMap.entrySet()){
            sb.append("DocID: ");
            sb.append(entry.getKey());
            sb.append("\n");
            for(Map.Entry<String, float[]> entry1: entry.getValue().entrySet()){
                sb.append("Term: ");
                sb.append(entry1.getKey());
                sb.append(" ");
                sb.append("IDF*boost: ");
                sb.append(entry1.getValue()[0]);
                sb.append(" ");
                sb.append("TF: ");
                sb.append(entry1.getValue()[1]);
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
