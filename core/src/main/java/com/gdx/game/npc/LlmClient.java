package com.gdx.game.npc;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LlmClient {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final String apiKey;
    private final JsonReader jsonReader = new JsonReader();

    public LlmClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public String ask(String systemPrompt, String userMessage) throws IOException {
        String body = buildRequestBody(systemPrompt, userMessage);
        String responseJson = postJson(body);
        return extractAnswerFromResponse(responseJson);
    }

    private String buildRequestBody(String systemPrompt, String userMessage) {
        return "{\n" +
                "  \"model\": \"llama-3.1-8b-instant\", \n" +
                "  \"temperature\": 0.7,\n" +
                "  \"max_tokens\": 256,\n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"system\", \"content\": " + jsonEscape(systemPrompt) + "},\n" +
                "    {\"role\": \"user\", \"content\": " + jsonEscape(userMessage) + "}\n" +
                "  ]\n" +
                "}";
    }

    private String jsonEscape(String s) {
        if (s == null) return "\"\"";
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    private String postJson(String body) throws IOException {
        HttpURLConnection conn = getHttpURLConnection(body);

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = readAll(is);

        if (code < 200 || code >= 300) {
            throw new IOException("Groq API error " + code + ": " + response);
        }

        return response;
    }

    private HttpURLConnection getHttpURLConnection(String body) throws IOException {
        URL url = new URL(LlmClient.API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        byte[] out = body.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(out.length);

        conn.connect();
        try (OutputStream os = conn.getOutputStream()) {
            os.write(out);
        }
        return conn;
    }

    private String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String extractAnswerFromResponse(String json) {
        JsonValue root = jsonReader.parse(json);
        JsonValue choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.size == 0) {
            return "Помилка: модель не повернула жодної відповіді.";
        }

        JsonValue first = choices.get(0);
        JsonValue message = first.get("message");
        if (message == null) {
            return "Помилка: некоректний формат відповіді моделі.";
        }

        String content = message.getString("content", "").trim();
        if (content.isEmpty()) {
            return "Модель нічого не відповіла.";
        }

        return content;
    }
}
