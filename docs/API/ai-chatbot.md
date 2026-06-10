# AI Chatbot API

Base URL: `/api/v1/chatbot`

API cung cấp tính năng chat với trợ lý ảo (AI) để tư vấn lộ trình học tập và gợi ý khóa học, tự động đưa ra các Course IDs để Frontend có thể hiển thị dưới dạng Carousel hoặc tích hợp nút "Thêm vào giỏ hàng".

---

## 1. Khởi tạo phiên chat (Create Session)

- **Endpoint**: `POST /sessions`
- **Auth**: Require `Bearer Token`
- **Response**:

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "title": "New Conversation"
  }
}
```

---

## 2. Lấy lịch sử chat (Get Messages)

- **Endpoint**: `GET /sessions/{id}/messages`
- **Auth**: Require `Bearer Token`
- **Response**:

```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "role": "USER",
      "content": "Tôi muốn học về Data",
      "recommendedCourses": null,
      "createdAt": "2026-06-10T10:00:00"
    },
    {
      "id": 2,
      "role": "ASSISTANT",
      "content": "Đây là các khóa học Data Analyst...",
      "recommendedCourses": "[1, 5, 8]",
      "createdAt": "2026-06-10T10:00:05"
    }
  ]
}
```

---

## 3. Gửi tin nhắn & Nhận tư vấn (Send Message)

- **Endpoint**: `POST /sessions/{id}/messages`
- **Auth**: Require `Bearer Token`
- **Request Body**:

```json
{
  "content": "Tôi muốn trở thành Data Engineer"
}
```

- **Response**:

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "replyMessage": "Để làm Data Engineer, bạn cần học SQL, Python và Spark. Tôi gợi ý 3 khóa học bên dưới:",
    "recommendedCourseIds": [10, 22, 15]
  }
}
```

---

## Logic Backend Nội Bộ (Context Compaction)

- Chatbot hỗ trợ lưu ngữ cảnh (Memory) qua Database.
- Khi một phiên chat (Session) đạt tới **bội số của 10 tin nhắn** (10, 20, 30...), hệ thống sẽ tự động gọi AI (LLM) để **tóm tắt** lịch sử chat và lưu vào `summary`.
- Các lượt chat sau, Backend chỉ gửi `summary` + các tin nhắn mới nhất, giúp tiết kiệm Token và tránh lỗi đầy Context Window của AI.
