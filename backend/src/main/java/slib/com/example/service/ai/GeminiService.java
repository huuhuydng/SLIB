package slib.com.example.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import slib.com.example.entity.ai.AIConfigEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to interact with Google Gemini AI API
 */
@Service
public class GeminiService {

    @Autowired
    private AIConfigService aiConfigService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    private final WebClient webClient;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public GeminiService() {
        this.webClient = WebClient.builder()
                .baseUrl(GEMINI_API_URL)
                .build();
    }

    /**
     * Generate response from Gemini AI
     * 
     * @param userMessage The user's question
     * @param chatHistory Previous messages for context
     * @return AI response with confidence score
     */
    public GeminiResponse generateResponse(String userMessage, List<Map<String, String>> chatHistory) {
        String apiKey = aiConfigService.getDecryptedApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return new GeminiResponse("Lỗi: Chưa cấu hình API key cho AI.", 0.0, true);
        }

        AIConfigEntity config = aiConfigService.getConfig().orElse(null);
        if (config == null) {
            return new GeminiResponse("Lỗi: Chưa có cấu hình AI.", 0.0, true);
        }

        // Build full prompt
        String fullPrompt = buildFullPrompt(config, userMessage, chatHistory);

        try {
            // Call Gemini API
            Map<String, Object> requestBody = buildRequestBody(fullPrompt, config);

            // Use full URL instead of relying on baseUrl
            String fullUrl = GEMINI_API_URL + config.getModel() + ":generateContent?key=" + apiKey;

            String response = WebClient.create()
                    .post()
                    .uri(fullUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseGeminiResponse(response);

        } catch (Exception e) {
            String errorMsg = "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau hoặc liên hệ thủ thư để được hỗ trợ.";
            return new GeminiResponse(errorMsg, 0.0, true);
        }
    }

    /**
     * Test API connection
     */
    public boolean testConnection() {
        String apiKey = aiConfigService.getDecryptedApiKey();
        System.out.println("[GeminiService] Testing connection...");
        System.out.println("[GeminiService] API Key present: " + (apiKey != null && !apiKey.isEmpty()));

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("[GeminiService] ERROR: API key is null or empty");
            return false;
        }

        System.out.println("[GeminiService] API Key length: " + apiKey.length());
        System.out.println(
                "[GeminiService] API Key preview: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");

        AIConfigEntity config = aiConfigService.getConfig().orElse(null);
        String model = config != null ? config.getModel() : "gemini-2.0-flash";

        System.out.println("[GeminiService] Using model: " + model);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(
                    Map.of("parts", List.of(Map.of("text", "Hello, respond with just 'OK'")))));

            // Use full URL instead of relying on baseUrl
            String fullUrl = GEMINI_API_URL + model + ":generateContent?key=" + apiKey;
            System.out.println("[GeminiService] Request URL: " + GEMINI_API_URL + model + ":generateContent?key=***");

            // Create a fresh WebClient for this request with full URL
            String response = WebClient.create()
                    .post()
                    .uri(fullUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("[GeminiService] Response received: "
                    + (response != null ? response.substring(0, Math.min(200, response.length())) + "..." : "null"));

            boolean success = response != null && !response.contains("\"error\"");
            System.out.println("[GeminiService] Connection test result: " + success);
            return success;

        } catch (Exception e) {
            System.err.println("[GeminiService] ERROR: Connection test failed with exception:");
            e.printStackTrace();
            return false;
        }
    }

    private String buildFullPrompt(AIConfigEntity config, String userMessage, List<Map<String, String>> chatHistory) {
        StringBuilder prompt = new StringBuilder();

        // System prompt
        prompt.append(config.getSystemPrompt()).append("\n\n");

        // Knowledge context (if enabled)
        if (Boolean.TRUE.equals(config.getEnableContext())) {
            prompt.append(knowledgeBaseService.buildKnowledgeContext());
        }

        // Chat history (if enabled)
        if (Boolean.TRUE.equals(config.getEnableHistory()) && chatHistory != null && !chatHistory.isEmpty()) {
            prompt.append("\n--- LỊCH SỬ HỘI THOẠI ---\n");
            for (Map<String, String> msg : chatHistory) {
                String role = msg.get("role").equals("user") ? "Sinh viên" : "AI";
                prompt.append(role).append(": ").append(msg.get("content")).append("\n");
            }
            prompt.append("--- HẾT LỊCH SỬ ---\n\n");
        }

        // User's current question
        prompt.append("Sinh viên hỏi: ").append(userMessage).append("\n\n");
        prompt.append(
                "Hãy trả lời câu hỏi trên. Nếu câu hỏi không liên quan đến thư viện hoặc bạn không chắc chắn, hãy bắt đầu câu trả lời bằng '[KHÔNG CHẮC CHẮN]'.");

        return prompt.toString();
    }

    private Map<String, Object> buildRequestBody(String prompt, AIConfigEntity config) {
        Map<String, Object> body = new HashMap<>();

        body.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))));

        body.put("generationConfig", Map.of(
                "temperature", config.getTemperature(),
                "maxOutputTokens", config.getMaxTokens()));

        return body;
    }

    private GeminiResponse parseGeminiResponse(String jsonResponse) {
        try {
            // Simple JSON parsing (in production, use proper JSON library)
            if (jsonResponse == null || jsonResponse.contains("\"error\"")) {
                return new GeminiResponse("Lỗi khi gọi AI. Vui lòng thử lại.", 0.0, true);
            }

            // Extract text from response
            // Response format: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
            int textStart = jsonResponse.indexOf("\"text\":\"");
            if (textStart == -1) {
                return new GeminiResponse("Không nhận được phản hồi từ AI.", 0.0, true);
            }

            textStart += 8; // Length of "\"text\":\""
            int textEnd = jsonResponse.indexOf("\"", textStart);
            if (textEnd == -1) {
                // Handle multiline text - find the end more carefully
                textEnd = jsonResponse.indexOf("\"}]}", textStart);
            }

            String text = jsonResponse.substring(textStart, textEnd)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");

            // Check if AI is uncertain
            boolean needsReview = text.contains("[KHÔNG CHẮC CHẮN]");
            double confidence = needsReview ? 0.3 : 0.9;

            // Remove uncertainty marker from response
            text = text.replace("[KHÔNG CHẮC CHẮN]", "").trim();

            return new GeminiResponse(text, confidence, needsReview);

        } catch (Exception e) {
            return new GeminiResponse("Lỗi xử lý phản hồi AI: " + e.getMessage(), 0.0, true);
        }
    }

    /**
     * Response wrapper class
     */
    public static class GeminiResponse {
        private final String content;
        private final Double confidenceScore;
        private final boolean needsReview;

        public GeminiResponse(String content, Double confidenceScore, boolean needsReview) {
            this.content = content;
            this.confidenceScore = confidenceScore;
            this.needsReview = needsReview;
        }

        public String getContent() {
            return content;
        }

        public Double getConfidenceScore() {
            return confidenceScore;
        }

        public boolean isNeedsReview() {
            return needsReview;
        }
    }
}
