import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class GoogUploadResponse {
    public final String uri;

    public GoogUploadResponse(@JsonProperty("file") Map<Object, Object> file) {
        this.uri = file.get("uri").toString();
    }
}