import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Map;

public class GoogGenerateResponse {
    public final String text;
    public final int tokens;

    public GoogGenerateResponse(@JsonProperty("candidates") ArrayList<Map<Object, Object>> candidates, @JsonProperty("usageMetadata") Map<String, Integer> meta) {
        Map<Object, Object> m = (Map<Object, Object>)candidates.get(0).get("content");
        ArrayList<Map<String, String>> a = (ArrayList<Map<String, String>>)m.get("parts");
        this.text = a.get(0).get("text");
        this.tokens = meta.get("totalTokenCount");
    }
}