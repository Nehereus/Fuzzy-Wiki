package edu.uci.cs230.team10.libFuzzyWiki;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocTermInfo {
    // Map<title, Map<Term, [IDF*boost, TF]>>;
    public final Map<String, Map<String, float[]>> infoMap = new HashMap<>();
    // Map<title, Text>;
    public final Map<String, String> textMap = new HashMap<>();

    public final Map<String, Float> weightMap = new HashMap<>(); // average of IDF*boost for each term;

    private int timeUsed = 0; // time used for the search in ms, a very bad code style for convenience

    public DocTermInfo( Map<String, Map<String, float[]>> infoMap, Map<String, Float> weightMap, Map<String, String> textMap) {
        this.infoMap.putAll(infoMap);
        this.weightMap.putAll(weightMap);
        this.textMap.putAll(textMap);
    }

    public DocTermInfo() {}

    public JSONObject toJson() {
        JSONObject res = new JSONObject();
        res.put("infoMap", new JSONObject(infoMap));
        res.put("weightMap", new JSONObject(weightMap));
        res.put("textMap", new JSONObject(textMap));
        res.put("timeUsed", timeUsed);
        return res;
    }
    public static DocTermInfo from(JSONObject json) {
        DocTermInfo res = new DocTermInfo();

        JSONObject infoMapJson = json.getJSONObject("infoMap");
        for (String doc : infoMapJson.keySet()) {
            var termMapJson = infoMapJson.getJSONObject(doc);
            Map<String, float[]> termMap = new HashMap<>();
            for (String term : termMapJson.keySet()) {
                List<Object> values = termMapJson.getJSONArray(term).toList();
                termMap.put(term, new float[]{
                        ((Number) values.get(0)).floatValue(),
                        ((Number) values.get(1)).floatValue()
                });
            }
            res.infoMap.put(doc, termMap);
        }
        var weightMapJson = json.getJSONObject("weightMap");
        for (String term : weightMapJson.keySet()) {
            res.weightMap.put(term, weightMapJson.getFloat(term));
        }

        var textMapJson = json.getJSONObject("textMap");
        for (String doc : textMapJson.keySet()) {
            res.textMap.put(doc, textMapJson.getString(doc));
        }
        res.timeUsed = json.getInt("timeUsed");

        return res;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Float> entry : weightMap.entrySet()) {
            sb.append("Term: ");
            sb.append(entry.getKey());
            sb.append(" ");
            sb.append("AvgIDF*boost: ");
            sb.append(entry.getValue());
            sb.append("\n");
        }
        for (Map.Entry<String, Map<String, float[]>> entry : infoMap.entrySet()) {
            sb.append("DocID: ");
            sb.append(entry.getKey());
            sb.append("\n");
            for (Map.Entry<String, float[]> entry1 : entry.getValue().entrySet()) {
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

    public int getTimeUsed() {
        return timeUsed;
    }

    public void setTimeUsed(int timeUsed) {
        this.timeUsed = timeUsed;
    }
}
