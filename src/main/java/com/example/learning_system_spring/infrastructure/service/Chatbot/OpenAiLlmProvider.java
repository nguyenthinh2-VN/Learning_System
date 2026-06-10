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
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiLlmProvider implements LlmProvider {

    @Value("${ai.openai.api-key:your-shopaikey-api-key}")
    private String apiKey;

    @Value("${ai.openai.base-url:https://api.shopaikey.com/v1}")
    private String baseUrl;

    @Value("${ai.openai.model:gpt-4o}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiLlmProvider() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public ChatResponse chat(List<Message> history, List<CourseContext> context, String userMessage) {
        String url = baseUrl + "/chat/completions";
        
        // System Prompt including Context and JSON schema requirement
        String systemPrompt = buildSystemPrompt(context);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        
        for (Message msg : history) {
            if (msg.getRole() == ChatRole.SYSTEM) {
                messages.add(Map.of("role", "system", "content", msg.getContent()));
            } else {
                String role = msg.getRole() == ChatRole.USER ? "user" : "assistant";
                messages.add(Map.of("role", role, "content", msg.getContent()));
            }
        }
        
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1000);
        requestBody.put("temperature", 1.0);
        requestBody.put("response_format", Map.of("type", "json_object")); // Require JSON format

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseOpenAiResponse(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to call ShopAIKey/OpenAI API", e);
        }
    }

    @Override
    public String summarize(List<Message> history) {
        String url = baseUrl + "/chat/completions";
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "Hãy tóm tắt ngắn gọn, đầy đủ thông tin cuộc hội thoại sau trong 2-3 câu."));
        
        for (Message msg : history) {
            if (msg.getRole() == ChatRole.SYSTEM) continue;
            String role = msg.getRole() == ChatRole.USER ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", msg.getContent()));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini"); // Cheaper model for summarization
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call ShopAIKey/OpenAI API for summarization", e);
        }
    }

    private String serializeContext(List<CourseContext> context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private ChatResponse parseOpenAiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            
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
            throw new RuntimeException("Failed to parse OpenAI JSON response", e);
        }
    }

    private String buildSystemPrompt(List<CourseContext> context) {
        return "Bạn là trợ lý ảo AI chuyên nghiệp của nền tảng học trực tuyến.\n" +
               "Nhiệm vụ của bạn là tư vấn lộ trình học tập, giải đáp thắc mắc về khóa học, và đưa ra lời khuyên hữu ích cho người học.\n\n" +
               "--- HƯỚNG DẪN HOẠT ĐỘNG ---\n" +
               "1. LUÔN LUÔN thân thiện, nhiệt tình và sử dụng tiếng Việt tự nhiên.\n" +
               "2. KHÔNG BAO GIỜ bịa đặt khóa học không tồn tại. Chỉ gợi ý các khóa học có trong ngữ cảnh (Context) bên dưới.\n" +
               "3. Nếu học viên hỏi những vấn đề KHÔNG liên quan đến học tập, IT, lập trình hoặc hệ thống: Hãy từ chối khéo léo và điều hướng họ về chủ đề học tập.\n" +
               "4. Bạn là nhân viên của hệ thống, không phải ChatGPT. Đừng nhắc đến OpenAI hay ShopAIKey.\n" +
               "5. LUÔN LUÔN trả về phản hồi dưới dạng chuẩn JSON Object duy nhất, không kèm theo bất kỳ văn bản markdown nào (vd: không có ```json). Định dạng:\n" +
               "{\n" +
               "  \"reply_message\": \"Câu trả lời hoặc tư vấn chi tiết của bạn dành cho học viên\",\n" +
               "  \"recommended_course_ids\": [ID1, ID2] // Mảng số nguyên chứa ID các khóa học bạn muốn gợi ý. Nếu không gợi ý, trả về mảng rỗng []\n" +
               "}\n\n" +
               "--- CONTEXT KHÓA HỌC HIỆN CÓ ---\n" +
               "Dưới đây là danh sách các khóa học trên hệ thống (bạn dùng để tìm `ID` và gợi ý):\n" +
               serializeContext(context);
    }
}
