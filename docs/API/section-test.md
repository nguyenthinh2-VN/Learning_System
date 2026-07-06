# Section Test API

Quản lý bài kiểm tra của chương học (Section Test). 
Bài kiểm tra sử dụng cấu trúc JSON để lưu trữ câu hỏi và đáp án.

## 1. Lấy Bài Test Của Chương (Dành cho Học Viên / Client)
**GET** `/api/v1/sections/{sectionId}/test`

Lấy danh sách các câu hỏi của bài test. Đáp án đúng (`correctAnswer`) được ẩn đi.
Yêu cầu quyền: `MEMBER`, `INSTRUCTOR`, `STAFF`, `ADMIN_USER`, `SUPER_ADMIN`.

**Path Parameters:**
- `sectionId` (Long): ID của chương học.

**Response (200 OK):**
```json
{
  "code": 200,
  "status": "OK",
  "data": [
    {
      "question": "Nội dung câu hỏi?",
      "options": ["A. Đáp án 1", "B. Đáp án 2", "C. Đáp án 3", "D. Đáp án 4"]
    }
  ],
  "timestamp": "2026-07-06T10:00:00"
}
```

## 2. Nộp Bài Test
**POST** `/api/v1/sections/{sectionId}/submit-test`

Nộp bài test và nhận kết quả điểm số. Nếu đúng từ 50% trở lên sẽ được đánh dấu hoàn thành.
Yêu cầu quyền: `MEMBER`, `INSTRUCTOR`, `STAFF`, `ADMIN_USER`, `SUPER_ADMIN`.

**Path Parameters:**
- `sectionId` (Long): ID của chương học.

**Request Body:**
```json
{
  "answers": {
    "0": "A. Đáp án 1",
    "1": "C. Đáp án 3"
  }
}
```

**Response (200 OK):**
```json
{
  "code": 200,
  "status": "OK",
  "data": {
    "score": 50.0,
    "passed": true,
    "correctCount": 1,
    "totalQuestions": 2
  },
  "timestamp": "2026-07-06T10:05:00"
}
```

## 3. Soạn Đề Thi (Dành cho Admin/Giảng Viên)
**PUT** `/api/v1/admin/sections/{sectionId}/test`

Lưu cấu trúc đề thi (JSON) cho một chương học.
Yêu cầu quyền: `INSTRUCTOR`, `STAFF`, `ADMIN_USER`, `SUPER_ADMIN` (kèm theo quyền `EDIT_SECTION`).

**Path Parameters:**
- `sectionId` (Long): ID của chương học.

**Request Headers:**
- `Content-Type`: `text/plain`

**Request Body:**
Chuỗi JSON thô chứa danh sách câu hỏi:
```json
[
  {
    "question": "Nội dung câu hỏi?",
    "options": ["A. Đáp án 1", "B. Đáp án 2", "C. Đáp án 3", "D. Đáp án 4"],
    "correctAnswer": "A. Đáp án 1"
  }
]
```

**Response (200 OK):**
```json
{
  "code": 200,
  "status": "OK",
  "data": "Cập nhật bài test thành công",
  "timestamp": "2026-07-06T10:10:00"
}
```

## Các mã lỗi (ErrorCode) thường gặp
- `SECTION_TEST_NOT_FOUND` (404): Không tìm thấy bài test cho chương học này.
- `INVALID_TEST_FORMAT` (400): Định dạng JSON của bài test bị lỗi hoặc không hợp lệ.
- `TEST_HAS_NO_QUESTIONS` (400): Bài test không có câu hỏi nào.
