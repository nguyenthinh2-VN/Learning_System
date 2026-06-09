package com.example.learning_system_spring.infrastructure.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tiện ích ký / xác thực chữ ký theo chuẩn VNPay (HMAC-SHA512).
 *
 * Quy tắc chuẩn của VNPay (rất dễ sai nếu làm tắt):
 *  - Sắp xếp toàn bộ field bắt đầu bằng "vnp_" theo thứ tự alphabet (TreeMap).
 *  - Bỏ qua field rỗng/null, bỏ qua "vnp_SecureHash" và "vnp_SecureHashType".
 *  - URL-encode CẢ key lẫn value bằng US-ASCII khi build chuỗi hashData và query.
 *  - hashData (để ký) và queryString (để redirect) build từ CÙNG tập field, cùng cách encode.
 *  - vnp_SecureHash = HMAC-SHA512(hashSecret, hashData), so sánh không phân biệt hoa/thường.
 *
 * Class thuần (không phụ thuộc Spring) để dễ unit test với secret giả.
 */
public final class VnPaySignature {

    private VnPaySignature() {}

    /**
     * Build query string đã ký để redirect user sang VNPay.
     *
     * @param params     các field vnp_* (chưa có vnp_SecureHash)
     * @param hashSecret secret sandbox/production
     * @return query string hoàn chỉnh: "field1=enc&field2=enc&...&vnp_SecureHash=xxx"
     */
    public static String buildSignedQuery(Map<String, String> params, String hashSecret) {
        TreeMap<String, String> sorted = sortedNonEmpty(params);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            String encKey = encode(e.getKey());
            String encValue = encode(e.getValue());
            if (!first) {
                hashData.append('&');
                query.append('&');
            }
            hashData.append(encKey).append('=').append(encValue);
            query.append(encKey).append('=').append(encValue);
            first = false;
        }

        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);
        return query.toString();
    }

    /**
     * Xác thực chữ ký của params trả về từ VNPay (return URL hoặc IPN).
     *
     * @param params     toàn bộ vnp_* nhận được (gồm cả vnp_SecureHash)
     * @param hashSecret secret
     * @return true nếu chữ ký khớp
     */
    public static boolean verify(Map<String, String> params, String hashSecret) {
        if (params == null) return false;
        String received = params.get("vnp_SecureHash");
        if (received == null || received.isBlank()) return false;

        TreeMap<String, String> sorted = sortedNonEmpty(params);
        // Loại field chữ ký ra khỏi dữ liệu ký
        sorted.remove("vnp_SecureHash");
        sorted.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (!first) hashData.append('&');
            hashData.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
            first = false;
        }

        String expected = hmacSHA512(hashSecret, hashData.toString());
        return expected.equalsIgnoreCase(received);
    }

    /** Sắp xếp alphabet + loại field null/rỗng. */
    private static TreeMap<String, String> sortedNonEmpty(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        if (params == null) return sorted;
        for (Map.Entry<String, String> e : params.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            if (key != null && key.startsWith("vnp_") && value != null && !value.isEmpty()) {
                sorted.put(key, value);
            }
        }
        return sorted;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    /** HMAC-SHA512 → hex string thường. */
    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(keySpec);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tính HMAC-SHA512", ex);
        }
    }

    /** Tiện ích nội bộ (không bắt buộc dùng) — giữ để tham chiếu thứ tự field. */
    static List<String> sortedKeys(Map<String, String> params) {
        return new ArrayList<>(sortedNonEmpty(params).keySet());
    }
}
