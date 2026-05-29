package com.example.learning_system_spring.application.port;

/**
 * Port lưu trữ file — abstraction không phụ thuộc framework (không biết MultipartFile/HTTP).
 *
 * Hiện tại implement bằng LocalFileStorageService (lưu trên ổ đĩa BE).
 * Khi cần chuyển cloud chỉ thêm implementation mới (S3FileStorageService...),
 * KHÔNG sửa interface này hay use case.
 */
public interface FileStorageService {

    /**
     * Lưu file và trả về public URL để truy cập.
     *
     * @param content     nội dung file (bytes)
     * @param contentType MIME type ĐÃ được validate (vd: image/png)
     * @param subDir      thư mục con phân loại (vd: "avatars")
     * @return public URL đầy đủ để truy cập file
     */
    String store(byte[] content, String contentType, String subDir);
}
