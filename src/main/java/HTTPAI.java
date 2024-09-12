import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.dreambot.api.utilities.Logger.log;

public class HTTPAI {
    private HttpClient client = HttpClient.newHttpClient();
    private String model = "gemini-1.5-flash";
    private String apiKey = "AIzaSyBwhd6oslqoXlDKQKHHfu3137K3KNx8Gsc";
    private int contextSize = 8192;
    private AIHistory history = new AIHistory();

    public String format() {
        return history.format();
    }

    public String[] prompt(String p, BufferedImage img) {
        String url;
        try {
            url = uploadImage(img);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        history.add("user", p, url);

        ObjectMapper mapper = new ObjectMapper();
        String json;
        try {
            json = mapper.writeValueAsString(history);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log(json);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log(response.statusCode());
        String gs = response.body();
        //log(gs);
        GoogGenerateResponse g = null;
        try {
            g = mapper.readValue(gs, GoogGenerateResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String text = g.text;
        history.add("model", text, null);
        log(text);
        if (g.tokens >= contextSize) {
            history.remove(2);
            history.remove(2);
        }
        log(g.tokens);
        String[] result = text.substring(text.indexOf('`') + 1, text.lastIndexOf('`')).split(",\\s+");
        log(Arrays.toString(result));
        return result;
    }

    private String uploadImage(BufferedImage img) throws IOException, InterruptedException {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        ImageIO.write(img, "png", s);
        byte[] png = s.toByteArray();

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/upload/v1beta/files?key=" + apiKey))
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", Integer.toString(png.length))
                .header("X-Goog-Upload-Heade-Content-Type", "image/png")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{'file': {'display_name': 'SCREENSHOT'}}"))
                .build();

        HttpResponse response = client.send(req, HttpResponse.BodyHandlers.discarding());

        String url = response.headers().firstValue("x-goog-upload-url").orElseThrow();

        req = HttpRequest.newBuilder(URI.create(url))
                .header("X-Goog-Upload-Offset", Integer.toString(0))
                .header("X-Goog-Upload-Command", "upload, finalize")
                .POST(HttpRequest.BodyPublishers.ofByteArray(png))
                .build();

        HttpResponse<Supplier<GoogUploadResponse>> response2 = client.send(req, new JsonBodyHandler<>(GoogUploadResponse.class));

        return response2.body().get().uri;
    }
}
