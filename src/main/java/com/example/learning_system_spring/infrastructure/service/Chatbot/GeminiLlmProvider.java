package com.example.learning_system_spring.infrastructure.service.Chatbot;

import com.example.learning_system_spring.application.port.LlmProvider;
import com.example.learning_system_spring.domain.model.ChatRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiLlmProvider implements LlmProvider {

    @Value("${ai.gemini.api-key:your-shopaikey-api-key}")
    private String apiKey;

    @Value("${ai.gemini.base-url:https://api.shopaikey.com}")
    private String baseUrl;

    @Value("${ai.gemini.model:gemini-2.5-flash}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiLlmProvider() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public ChatResponse chat(List<Message> history, List<CourseContext> context, String userMessage) {
        // Build Endpoint URL for Gemini
        String url = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        String systemPrompt = buildSystemPrompt(context);

        Map<String, Object> requestBody = new HashMap<>();

        // 1. System Instruction
        requestBody.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        ));

        // 2. Contents (History + User Message)
        List<Map<String, Object>> contents = new ArrayList<>();
        
        for (Message msg : history) {
            if (msg.getRole() == ChatRole.SYSTEM) continue; // Skip since Gemini uses separate systemInstruction
            String role = msg.getRole() == ChatRole.USER ? "user" : "model";
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", msg.getContent()))
            ));
        }

        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        requestBody.put("contents", contents);

        // 3. Generation Config (Require JSON)
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 1.0);
        generationConfig.put("maxOutputTokens", 1000);
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseGeminiResponse(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to call ShopAIKey/Gemini API", e);
        }
    }

    @Override
    public String summarize(List<Message> history) {
        String url = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", "Hãy tóm tắt ngắn gọn, đầy đủ thông tin cuộc hội thoại sau trong 2-3 câu."))
        ));

        List<Map<String, Object>> contents = new ArrayList<>();
        for (Message msg : history) {
            if (msg.getRole() == ChatRole.SYSTEM) continue;
            String role = msg.getRole() == ChatRole.USER ? "user" : "model";
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", msg.getContent()))
            ));
        }
        requestBody.put("contents", contents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call ShopAIKey/Gemini API for summarization", e);
        }
    }

    private String serializeContext(List<CourseContext> context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private ChatResponse parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
            JsonNode parsedContent = objectMapper.readTree(content);
            String replyMessage = parsedContent.path("reply_message").asText();
            
            List<Long> recommendedCourseIds = new ArrayList<>();
            JsonNode idsNode = parsedContent.path("recommended_course_ids");
            if (idsNode.isArray()) {
                for (JsonNode idNode : idsNode) {
                    recommendedCourseIds.add(idNode.asLong());
                }
            }
            
            return ChatResponse.builder()
                    .replyMessage(replyMessage)
                    .recommendedCourseIds(recommendedCourseIds)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini JSON response", e);
        }
    }

    private String buildSystemPrompt(List<CourseContext> context) {
        return "Bạn là trợ lý ảo AI chuyên nghiệp của nền tảng học trực tuyến.\n" +
               "Nhiệm vụ của bạn là tư vấn lộ trình học tập, giải đáp thắc mắc về khóa học, và đưa ra lời khuyên hữu ích cho người học.\n\n" +
               "--- HƯỚNG DẪN HOẠT ĐỘNG ---\n" +
               "1. LUÔN LUÔN thân thiện, nhiệt tình và sử dụng tiếng Việt tự nhiên.\n" +
               "2. KHÔNG BAO GIỜ bịa đặt khóa học không tồn tại. Chỉ gợi ý các khóa học có trong ngữ cảnh (Context) bên dưới.\n" +
               "3. Nếu học viên hỏi những vấn đề KHÔNG liên quan đến học tập, IT, lập trình hoặc hệ thống: Hãy từ chối khéo léo và điều hướng họ về chủ đề học tập.\n" +
               "4. Bạn là nhân viên của hệ thống. Đừng nhắc đến AI hay ShopAIKey.\n" +
               "5. Yêu cầu BẮT BUỘC trả về JSON chuẩn, không có markdown text. Định dạng:\n" +
               "{\n" +
               "  \"reply_message\": \"Câu trả lời tư vấn của bạn\",\n" +
               "  \"recommended_course_ids\": [ID1, ID2] // Mảng số nguyên ID khóa học. Nếu không có thì []\n" +
               "}\n\n" +
               "--- CONTEXT KHÓA HỌC HIỆN CÓ ---\n" +
               "Danh sách khóa học trên hệ thống:\n" +
               serializeContext(context);
    }
}
