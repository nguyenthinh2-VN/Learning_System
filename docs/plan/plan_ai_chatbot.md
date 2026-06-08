# Kế Hoạch Chức Năng: AI Chatbot Tư Vấn Lộ Trình Học Tập

## 1. Mục tiêu
Tích hợp một Trợ lý ảo (AI Chatbot) vào hệ thống LMS để tư vấn lộ trình học tập cá nhân hóa. Trợ lý này sẽ gọi API của bên thứ 3. Khi user đồng ý với gợi ý, các khóa học sẽ được đưa vào **Giỏ hàng (Cart)** để chờ thanh toán.

---

## 2. Kiến trúc Hệ thống (Architecture)

### 2.1. Abstract Interface cho LLM Provider
Hệ thống sẽ áp dụng Design Pattern **Strategy / Adapter** để không bị phụ thuộc vào một hãng AI duy nhất:
- Tạo interface `LlmProvider` có phương thức `String chat(List<ChatMessage> history, List<CourseDto> context)`.
- Cài đặt (Implementations): `OpenAiLlmProvider`, `GeminiLlmProvider`, `ClaudeLlmProvider`.
- Dùng cấu hình (`application.yml` `ai.provider=gemini`) để quyết định Inject bean nào lúc runtime.

### 2.2. Context Injection (Cung cấp ngữ cảnh)
Backend query DB lấy danh sách các khóa học liên quan (hoặc toàn bộ nếu ít) và đưa vào System Prompt để AI tư vấn dựa trên dữ liệu thật của hệ thống.

### 2.3. Dữ liệu Trả về (JSON)
AI phải trả về định dạng JSON:
```json
{
  "reply_message": "Dựa trên nhu cầu làm Data Analyst, tôi gợi ý 3 khóa học sau...",
  "recommended_course_ids": [15, 22, 8]
}
```

---

## 3. Thiết kế Database (Lưu Lịch sử)

1. **`chat_sessions`**:
   - `id`, `user_id`, `title`, `created_at`
2. **`chat_messages`**:
   - `id`, `session_id`, `role` (USER/ASSISTANT), `content`
   - `recommended_courses` (JSON)

---

## 4. API Endpoints Dự Kiến

- `POST /api/v1/chatbot/sessions`: Khởi tạo phiên chat.
- `GET /api/v1/chatbot/sessions/{id}/messages`: Xem lại lịch sử chat.
- `POST /api/v1/chatbot/sessions/{id}/messages`: Gửi tin nhắn mới. Trả về câu trả lời + danh sách thông tin khóa học (Card).

---

## 5. Luồng Nghiệp Vụ (Business Logic) Cập Nhật

1. Học viên chat với AI.
2. Backend dùng `LlmProvider` để lấy kết quả tư vấn JSON.
3. Trả về Frontend. Frontend hiển thị câu chat của AI kèm dạng Carousel các khóa học.
4. **THAY ĐỔI MỚI:** Thay vì gán trực tiếp, Frontend sẽ hiển thị nút **"Thêm Lộ Trình Này Vào Giỏ Hàng"**.
5. Khi bấm, toàn bộ ID khóa học gợi ý được gửi lên API `/api/v1/cart/items` của chức năng Giỏ hàng.
6. User vào giỏ hàng, xem lại giá tiền, loại bỏ khóa học không thích và bấm **Thanh toán hàng loạt (Bulk Checkout)**.

---

## > [!IMPORTANT]
## Trạng Thái: Đang Lên Kế Hoạch (Chưa Code)

Kế hoạch này đã được **cập nhật Abstract Interface** cho LLM và đổi luồng gán khóa học sang **Đẩy vào Giỏ Hàng**. 
Bạn hãy xem lại các thay đổi nhé.
