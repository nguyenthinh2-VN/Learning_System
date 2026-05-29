package com.example.learning_system_spring.infrastructure.storage;

import com.example.learning_system_spring.application.port.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Lưu file trên ổ đĩa local của BE.
 *
 * File được ghi vào {base-dir}/{subDir}/{uuid}.{ext} và phục vụ tĩnh qua
 * {public-base-url}/uploads/{subDir}/{uuid}.{ext} (xem WebMvcConfig + SecurityConfig).
 *
 * Tên file dùng UUID nội bộ, KHÔNG tin tên gốc từ client → chống path traversal.
 */
@Component
public class LocalFileStorageService implements FileStorageService {

    private static final Map<String, String> EXTENSION_BY_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final Path baseDir;
    private final String publicBaseUrl;

    public LocalFileStorageService(
            @Value("${storage.local.base-dir:./uploads}") String baseDir,
            @Value("${storage.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        // Bỏ dấu "/" thừa ở cuối để ghép URL nhất quán
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @Override
    public String store(byte[] content, String contentType, String subDir) {
        String ext = EXTENSION_BY_TYPE.getOrDefault(contentType, "bin");
        String filename = UUID.randomUUID() + "." + ext;

        try {
            Path targetDir = baseDir.resolve(subDir).normalize();
            // Đảm bảo không thoát ra ngoài baseDir
            if (!targetDir.startsWith(baseDir)) {
                throw new IllegalArgumentException("Thư mục lưu trữ không hợp lệ: " + subDir);
            }
            Files.createDirectories(targetDir);

            Path target = targetDir.resolve(filename);
            Files.write(target, content);

            return publicBaseUrl + "/uploads/" + subDir + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file: " + e.getMessage(), e);
        }
    }
}
