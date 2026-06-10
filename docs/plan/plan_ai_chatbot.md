# Kế Hoạch Chức Năng: AI Chatbot Tư Vấn Lộ Trình Học Tập

## 1. Mục tiêu
Tích hợp một Trợ lý ảo (AI Chatbot) vào hệ thống LMS để tư vấn lộ trình học tập cá nhân hóa. Trợ lý này sẽ gọi API của bên thứ 3. Khi user đồng ý với gợi ý, các khóa học sẽ được đưa vào **Giỏ hàng (Cart)** để chờ thanh toán.

---

## 2. Kiến trúc Hệ thống (Architecture) & Pattern Áp Dụng

Áp dụng chuẩn **Spring Boot Architecture** với các tầng Controller, Service, và Repository rành mạch, kết hợp với các nguyên tắc **SOLID** trong việc thiết kế AI Provider.

### 2.1. Abstract Interface cho LLM Provider (Strategy Pattern - OCP & DIP)
Hệ thống sẽ áp dụng Design Pattern **Strategy** để không bị phụ thuộc vào một hãng AI duy nhất, giúp code dễ dàng mở rộng (Open/Closed Principle - OCP) và phụ thuộc vào Abstraction (Dependency Inversion Principle - DIP).

```java
public interface LlmProvider {
    /**
     * @param history Lịch sử trò chuyện trước đó
     * @param context Ngữ cảnh (danh sách khóa học)
     * @param userMessage Tin nhắn hiện tại của user
     * @return Chuỗi JSON chứa câu trả lời và danh sách ID khóa học gợi ý
     */
    String chat(List<ChatMessageDto> history, List<CourseDto> context, String userMessage);
}
```

- **Implementations:** Tạo các class implement interface này như `GeminiLlmProvider`, `OpenAiLlmProvider`, `ClaudeLlmProvider`.
- **Dependency Injection:** Sử dụng Spring `@ConditionalOnProperty` để tự động inject Bean tương ứng dựa trên cấu hình trong `application.yml`.

```java
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiLlmProvider implements LlmProvider {
    // Implement logic gọi qua API của Gemini
}
```

### 2.2. Service Layer & Context Injection
Backend truy xuất CSDL lấy danh sách các khóa học liên quan (tên, mô tả ngắn, ID) và đưa vào System Prompt để AI tư vấn dựa trên dữ liệu thật của hệ thống LMS. Các thao tác lưu trữ được bọc trong `@Transactional`.

```java
@Service
public class ChatbotService {
    private final LlmProvider llmProvider;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final CourseRepository courseRepo;

    public ChatbotService(LlmProvider llmProvider, /*...*/) {
        this.llmProvider = llmProvider;
        // ...
    }

    @Transactional
    public ChatResponse sendMessage(Long sessionId, String userMessage) {
        // 1. Lấy ngữ cảnh khóa học và lịch sử chat
        // 2. Uỷ quyền cho llmProvider xử lý
        // 3. Lưu tin nhắn của User và phản hồi của AI vào Database
        // 4. Map Entity sang DTO trả về
    }
}
```

### 2.3. Dữ liệu Trả về (JSON Format)
AI phải được prompt để luôn trả về định dạng JSON chuẩn xác:
```json
{
  "reply_message": "Dựa trên nhu cầu làm Data Analyst, tôi gợi ý 3 khóa học sau...",
  "recommended_course_ids": [15, 22, 8]
}
```

---

## 3. Thiết kế Database (Lưu Lịch sử theo chuẩn JPA)

Sử dụng JPA Entity với quan hệ One-to-Many để quản lý phiên chat và tin nhắn.

1. **`ChatSessionEntity`** (`chat_sessions`):
   - `id` (PK), `user_id` (FK), `title`, `created_at`, `updated_at`
   - `summary` (TEXT) - Lưu trữ bản tóm tắt các đoạn hội thoại cũ (Context Compaction) để tiết kiệm token.
2. **`ChatMessageEntity`** (`chat_messages`):
   - `id` (PK), `session_id` (FK), `role` (Enum: USER / ASSISTANT / SYSTEM), `content` (TEXT)
   - `recommended_courses` (JSONB / VARCHAR lưu dạng JSON array IDs)
   - `created_at`

---

## 4. API Endpoints & RESTful Design

Thiết kế API tuân thủ REST, có sử dụng Record DTO validation và cấu trúc trả về đồng nhất (`ApiResponse`).

```java
@RestController
@RequestMapping("/api/v1/chatbot")
@Validated
public class ChatbotController {
    
    private final ChatbotService chatbotService;
    // constructor...

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession() {
        // Khởi tạo phiên chat mới
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(@PathVariable Long id) {
        // Lấy lịch sử đoạn hội thoại
    }

    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @PathVariable Long id, 
            @Valid @RequestBody SendMessageRequest request) {
        // Nhận tin nhắn user, xử lý qua LLM và trả về phản hồi
    }
}
```

**DTO Validation Pattern:**
```java
public record SendMessageRequest(
    @NotBlank(message = "Message content cannot be blank") 
    @Size(max = 2000, message = "Message is too long") 
    String content
) {}
```

---

## 5. Luồng Nghiệp Vụ (Business Logic) Cập Nhật

1. **Khởi tạo:** Học viên vào trang Chatbot, gọi API `/sessions` để tạo phiên hoặc load lại phiên cũ.
2. **Hỏi đáp:** Học viên gửi nhu cầu học tập qua `/sessions/{id}/messages`.
3. **AI Xử lý (Backend):**
   - **Tối ưu Token (Context Compaction):** Kiểm tra số lượng tin nhắn trong Session. Nếu vượt ngưỡng (VD: 10-20 tin), hệ thống sẽ gọi LLM để tóm tắt các tin nhắn cũ và lưu vào trường `summary` của Session.
   - Trích xuất lịch sử tin nhắn: Lấy `summary` (nhúng vào System Prompt) + vài tin nhắn mới nhất.
   - Inject context khóa học (RAG cơ bản).
   - Gọi logic xử lý linh động qua Interface `LlmProvider`.
   - Lưu trữ toàn bộ tương tác vào database (Transaction-safe).
4. **Frontend Hiển thị:** Trả về Frontend. Frontend hiển thị câu chat của AI kèm Carousel chứa Card các khóa học được gợi ý (dựa vào mảng `recommended_course_ids`).
5. **Đẩy vào Giỏ Hàng (Cart Integration):** Thay vì tự động gán, Frontend hiển thị nút CTA **"Thêm Lộ Trình Này Vào Giỏ Hàng"**.
6. **Thanh Toán:** Khi bấm nút, hệ thống gọi API `/api/v1/cart/items` thêm nhiều khóa học cùng lúc. User xem lại và bấm **Thanh toán hàng loạt (Bulk Checkout)**.

---

## > [!IMPORTANT]
## Trạng Thái: Đã Tinh Chỉnh & Áp Dụng Design Pattern

Kế hoạch này đã được tinh chỉnh với các **kỹ thuật chuẩn của Spring Boot** (Record DTO, Validation, Service Transactional) và tuân thủ chặt chẽ nguyên tắc **SOLID (OCP, DIP)**. Giao tiếp với AI hoàn toàn abstract, có thể plug-and-play bất kỳ Model LLM nào qua configuration. Sẵn sàng để đi vào cài đặt mã nguồn (implementation).
