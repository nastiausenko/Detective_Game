package com.gdx.game.shared.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LlmClient {
    public enum ModelTier {
        FAST,
        SMART
    }

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String OPENAI_FAST_MODEL = "gpt-5-mini";
    private static final String OPENAI_SMART_MODEL = "gpt-5";
    private static final String OPENAI_EMBEDDING_MODEL = "text-embedding-3-small";
    private static final int OPENAI_EMBEDDING_DIMENSIONS = 512;
    private static final String GROQ_MODEL = "openai/gpt-oss-120b";
    private static final int DEFAULT_MAX_TOKENS = 512;
    private static final int MAX_COMPLETION_TOKENS = 1024;
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int GROQ_MIN_COMPLETION_TOKENS = 640;

    private final String openAiKey;
    private final String groqKey;

    public LlmClient(String openAiKey, String groqKey) {
        this.openAiKey = openAiKey;
        this.groqKey = groqKey;
    }

    public String ask(String systemPrompt, String userMessage) throws IOException {
        return ask(systemPrompt, userMessage, DEFAULT_MAX_TOKENS, ModelTier.FAST);
    }

    public String ask(String systemPrompt, String userMessage, int maxTokens) throws IOException {
        return ask(systemPrompt, userMessage, maxTokens, ModelTier.FAST);
    }

    public String ask(String systemPrompt, String userMessage, int maxTokens, ModelTier modelTier) throws IOException {
        try {
            return askOpenAi(systemPrompt, userMessage, maxTokens, modelTier);
        } catch (IOException e) {
            Gdx.app.error("LLM", "OpenAI failed, falling back to Groq", e);
            Gdx.app.log("LLM", "Using Groq fallback model: " + GROQ_MODEL);

            try {
                return askGroq(systemPrompt, userMessage, maxTokens);
            } catch (IOException groqErr) {
                Gdx.app.error("LLM", "Groq fallback also failed", groqErr);
                throw groqErr;
            }
        }
    }

    public float[][] createEmbeddings(List<String> inputs) throws IOException {
        if (inputs == null || inputs.isEmpty()) {
            return new float[0][];
        }

        String body = buildOpenAiEmbeddingsBody(inputs);
        String responseJson = postJson(OPENAI_EMBEDDINGS_URL, openAiKey, body);
        return extractEmbeddingsFromResponse(responseJson, inputs.size());
    }

    private String askOpenAi(
        String systemPrompt,
        String userMessage,
        int maxTokens,
        ModelTier modelTier
    ) throws IOException {
        String body = buildOpenAiBody(systemPrompt, userMessage, maxTokens, modelTier);
        String responseJson = postJson(OPENAI_URL, openAiKey, body);
        return extractAnswerFromResponse(responseJson);
    }

    private String askGroq(String systemPrompt, String userMessage, int maxTokens) throws IOException {
        String body = buildGroqBody(systemPrompt, userMessage, maxTokens);
        String responseJson = postJson(GROQ_URL, groqKey, body);
        return extractAnswerFromResponse(responseJson);
    }

    private String buildOpenAiBody(
        String systemPrompt,
        String userMessage,
        int maxTokens,
        ModelTier modelTier
    ) {
        return "{\n" +
            "  \"model\": " + jsonEscape(resolveOpenAiModel(modelTier)) + ",\n" +
            "  \"temperature\": 1,\n" +
            "  \"max_completion_tokens\": " + sanitizeMaxTokens(maxTokens) + ",\n" +
            "  \"reasoning_effort\": \"minimal\",\n" +
            "  \"messages\": [\n" +
            "    {\"role\": \"system\", \"content\": " + jsonEscape(systemPrompt) + "},\n" +
            "    {\"role\": \"user\", \"content\": " + jsonEscape(userMessage) + "}\n" +
            "  ]\n" +
            "}";
    }

    private String buildGroqBody(String systemPrompt, String userMessage, int maxTokens) {
        return "{\n" +
            "  \"model\": " + jsonEscape(LlmClient.GROQ_MODEL) + ",\n" +
            "  \"temperature\": 1,\n" +
            "  \"max_tokens\": " + sanitizeGroqMaxTokens(maxTokens) + ",\n" +
            "  \"messages\": [\n" +
            "    {\"role\": \"system\", \"content\": " + jsonEscape(systemPrompt) + "},\n" +
            "    {\"role\": \"user\", \"content\": " + jsonEscape(userMessage) + "}\n" +
            "  ]\n" +
            "}";
    }

    private int sanitizeMaxTokens(int maxTokens) {
        if (maxTokens < 32) return 32;
        return Math.min(maxTokens, MAX_COMPLETION_TOKENS);
    }

    private int sanitizeGroqMaxTokens(int maxTokens) {
        return Math.max(sanitizeMaxTokens(maxTokens), GROQ_MIN_COMPLETION_TOKENS);
    }

    private String resolveOpenAiModel(ModelTier modelTier) {
        return modelTier == ModelTier.SMART ? OPENAI_SMART_MODEL : OPENAI_FAST_MODEL;
    }

    private String buildOpenAiEmbeddingsBody(List<String> inputs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
            .append("  \"model\": ").append(jsonEscape(OPENAI_EMBEDDING_MODEL)).append(",\n")
            .append("  \"encoding_format\": \"float\",\n")
            .append("  \"dimensions\": ").append(OPENAI_EMBEDDING_DIMENSIONS).append(",\n")
            .append("  \"input\": [\n");

        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append("    ").append(jsonEscape(inputs.get(i)));
        }

        sb.append("\n  ]\n")
            .append("}");

        return sb.toString();
    }

    private String jsonEscape(String s) {
        if (s == null) return "\"\"";
        String escaped = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    private String postJson(String url, String apiKey, String body) throws IOException {
        HttpURLConnection conn = getHttpURLConnection(url, apiKey, body);

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300)
            ? conn.getInputStream()
            : conn.getErrorStream();

        String response = readAll(is);

        if (code < 200 || code >= 300) {
            throw new IOException("LLM API error " + code + ": " + response);
        }

        return response;
    }

    private HttpURLConnection getHttpURLConnection(String urlString, String apiKey, String body) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
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

    private float[][] extractEmbeddingsFromResponse(String json, int expectedCount) throws IOException {
        JsonValue root = new JsonReader().parse(json);
        JsonValue data = root.get("data");

        if (data == null || !data.isArray()) {
            throw new IOException("Embeddings API response does not contain data array.");
        }

        float[][] embeddings = new float[expectedCount][];

        for (JsonValue item = data.child; item != null; item = item.next) {
            JsonValue indexNode = item.get("index");
            JsonValue embeddingNode = item.get("embedding");

            if (indexNode == null || embeddingNode == null || !embeddingNode.isArray()) {
                continue;
            }

            int index = indexNode.asInt();
            if (index < 0 || index >= expectedCount) {
                continue;
            }

            float[] vector = new float[embeddingNode.size];
            int vectorIndex = 0;
            for (JsonValue value = embeddingNode.child; value != null; value = value.next) {
                if (vectorIndex >= vector.length) break;
                vector[vectorIndex] = value.asFloat();
                vectorIndex++;
            }
            embeddings[index] = vector;
        }

        for (int i = 0; i < embeddings.length; i++) {
            if (embeddings[i] == null) {
                throw new IOException("Embeddings API response is missing vector at index " + i + ".");
            }
        }

        return embeddings;
    }

    private String extractAnswerFromResponse(String json) throws IOException {
        JsonValue root = new JsonReader().parse(json);
        JsonValue choices = root.get("choices");

        if (choices == null || !choices.isArray() || choices.size == 0) {
            throw new IOException("LLM response does not contain choices.");
        }

        JsonValue choice = choices.get(0);
        JsonValue message = choice.get("message");
        if (message == null) {
            throw new IOException("LLM response does not contain message.");
        }

        JsonValue contentNode = message.get("content");
        if (contentNode != null && contentNode.isString()) {
            String content = contentNode.asString().trim();
            if (!content.isEmpty()) {
                return content;
            }
        }

        if (contentNode != null && contentNode.isArray()) {
            StringBuilder combined = new StringBuilder();
            for (JsonValue part = contentNode.child; part != null; part = part.next) {
                JsonValue textNode = part.get("text");
                if (textNode == null) continue;

                if (textNode.isString()) {
                    combined.append(textNode.asString());
                } else if (textNode.isObject()) {
                    if (textNode.has("value") && textNode.get("value").isString()) {
                        combined.append(textNode.get("value").asString());
                    }
                }
            }
            String finalText = combined.toString().trim();
            if (!finalText.isEmpty()) {
                return finalText;
            }
        }

        JsonValue refusalNode = message.get("refusal");
        if (refusalNode != null && refusalNode.isString()) {
            String refusalText = refusalNode.asString().trim();
            if (!refusalText.isEmpty()) {
                return refusalText;
            }
        }

        Gdx.app.log("LLM_RAW", json);

        String finishReason = choice.getString("finish_reason", "");
        throw new IOException("LLM response contained no message content. finish_reason=" + finishReason);
    }
}
