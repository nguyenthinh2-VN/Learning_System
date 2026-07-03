# Từ điển Đa ngôn ngữ (i18n Translation Dictionary)

File này tổng hợp toàn bộ các `code` trả về từ Backend. Frontend (FE) và đội dịch thuật sẽ dựa vào file này để quản lý ngôn ngữ, đảm bảo việc việt hóa (VN) và dịch sang tiếng Trung phồn thể (TW) hoặc tiếng Anh (EN) luôn đồng bộ và không bị sót.

## 1. SUCCESS CODES (Mã Thành Công)

| Mã Code (BE trả về) | Tiếng Việt (VN) | Tiếng Trung Phồn Thể (TW) | Tiếng Anh (EN) |
| :--- | :--- | :--- | :--- |
| `SUCCESS` | Thành công | 成功 | Success |
| `CREATED` | Tạo thành công | 創建成功 | Created successfully |

---

## 2. ERROR CODES (Mã Lỗi)

### Auth & User
| Mã Code (BE trả về) | Tiếng Việt (VN) | Tiếng Trung Phồn Thể (TW) | Tiếng Anh (EN) |
| :--- | :--- | :--- | :--- |
| `INVALID_EMAIL` | Email không hợp lệ | 電子郵件無效 | Invalid email |
| `EMAIL_ALREADY_EXISTS` | Email đã tồn tại | 電子郵件已存在 | Email already exists |
| `USER_NOT_FOUND` | Không tìm thấy người dùng | 找不到用戶 | User not found |
| `INVALID_CREDENTIALS` | Sai thông tin đăng nhập | 登入資訊錯誤 | Invalid credentials |
| `INVALID_PASSWORD` | Mật khẩu không đúng | 密碼錯誤 | Invalid password |
| `ACCOUNT_DISABLED` | Tài khoản đã bị vô hiệu hóa | 帳號已停用 | Account is disabled |

### Roles & Permissions
| Mã Code (BE trả về) | Tiếng Việt (VN) | Tiếng Trung Phồn Thể (TW) | Tiếng Anh (EN) |
| :--- | :--- | :--- | :--- |
| `ROLE_NOT_FOUND` | Không tìm thấy Role | 找不到角色 | Role not found |
| `PERMISSION_NOT_FOUND` | Không tìm thấy quyền | 找不到權限 | Permission not found |
| `PROTECTED_ROLE` | Role này được bảo vệ | 此角色受保護 | Protected role |
| `ACCESS_DENIED` | Từ chối truy cập | 拒絕訪問 | Access denied |

### System & Validation
| Mã Code (BE trả về) | Tiếng Việt (VN) | Tiếng Trung Phồn Thể (TW) | Tiếng Anh (EN) |
| :--- | :--- | :--- | :--- |
| `INVALID_FILE_TYPE` | Định dạng file không hợp lệ | 檔案格式無效 | Invalid file type |
| `FILE_TOO_LARGE` | Dung lượng file quá lớn | 檔案過大 | File too large |
| `VALIDATION_ERROR` | Lỗi xác thực dữ liệu | 資料驗證錯誤 | Validation error |
| `INTERNAL_ERROR` | Lỗi máy chủ nội bộ | 內部伺服器錯誤 | Internal server error |
| `BAD_REQUEST` | Yêu cầu không hợp lệ | 錯誤的請求 | Bad request |

### Course (Khóa học)
| Mã Code (BE trả về) | Tiếng Việt (VN) | Tiếng Trung Phồn Thể (TW) | Tiếng Anh (EN) |
| :--- | :--- | :--- | :--- |
| `COURSE_NOT_FOUND` | Không tìm thấy khóa học | 找不到課程 | Course not found |
| `COURSE_NOT_PUBLISHED` | Khóa học chưa được xuất bản | 課程尚未發布 | Course not published |
| `COURSE_PRICE_LOCKED` | Giá khóa học đã bị khóa | 課程價格已鎖定 | Course price locked |
| `COURSE_ALREADY_PUBLISHED` | Khóa học đã được xuất bản | 課程已發布 | Course already published |

### Section & Lesson (Chương & Bài học)
| Mã Code (BE trả về) | Tiếng Việt (VN) | Tiếng Trung Phồn Thể (TW) | Tiếng Anh (EN) |
| :--- | :--- | :--- | :--- |
| `SECTION_NOT_FOUND` | Không tìm thấy chương học | 找不到章節 | Section not found |
| `SECTION_ACCESS_DENIED` | Không có quyền truy cập chương | 無權訪問章節 | Section access denied |
| `LESSON_NOT_FOUND` | Không tìm thấy bài học | 找不到課程單元 | Lesson not found |
| `LESSON_ACCESS_DENIED` | Không có quyền truy cập bài học | 無權訪問課程單元 | Lesson access denied |

### Voucher (Mã giảm giá)
| Mã Code (BE trả về) | Tiếng Việt (VN) | Tiếng Trung Phồn Thể (TW) | Tiếng Anh (EN) |
| :--- | :--- | :--- | :--- |
| `VOUCHER_NOT_FOUND` | Không tìm thấy mã giảm giá | 找不到優惠券 | Voucher not found |
| `VOUCHER_INACTIVE` | Mã giảm giá không hoạt động | 優惠券未啟用 | Voucher inactive |
| `VOUCHER_NOT_YET_ACTIVE` | Mã giảm giá chưa đến thời gian áp dụng | 優惠券尚未生效 | Voucher not yet active |
| `VOUCHER_EXPIRED` | Mã giảm giá đã hết hạn | 優惠券已過期 | Voucher expired |
| `VOUCHER_NOT_APPLICABLE` | Mã giảm giá không áp dụng cho đơn này | 優惠券不適用 | Voucher not applicable |
| `VOUCHER_MIN_ORDER_NOT_MET` | Đơn hàng chưa đạt giá trị tối thiểu | 未達最低消費金額 | Min order amount not met |
| `VOUCHER_USAGE_LIMIT_REACHED` | Mã giảm giá đã hết lượt sử dụng | 優惠券已達使用上限 | Usage limit reached |
| `VOUCHER_USAGE_PER_USER_EXCEEDED` | Bạn đã hết lượt dùng mã này | 您已超過使用次數限制 | User usage limit exceeded |
| `VOUCHER_USE_DENIED` | Không thể sử dụng mã giảm giá này | 無法使用此優惠券 | Voucher use denied |
| `VOUCHER_CODE_ALREADY_EXISTS` | Mã giảm giá đã tồn tại | 優惠券代碼已存在 | Voucher code already exists |
| `VOUCHER_USAGE_LIMIT_TOO_LOW` | Giới hạn sử dụng quá thấp | 使用限制過低 | Usage limit too low |
| `VOUCHER_IMMUTABLE_FIELD` | Không thể sửa đổi trường này của Voucher | 無法修改此欄位 | Immutable field |

### Purchase (Mua khóa học / Ví)
| Mã Code (BE trả về) | Tiếng Việt (VN) | Tiếng Trung Phồn Thể (TW) | Tiếng Anh (EN) |
| :--- | :--- | :--- | :--- |
| `ALREADY_ENROLLED` | Bạn đã tham gia khóa học này | 您已報名此課程 | Already enrolled |
| `INSUFFICIENT_BALANCE` | Số dư không đủ | 餘額不足 | Insufficient balance |
