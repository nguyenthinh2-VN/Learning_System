package com.example.learning_system_spring.infrastructure.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho VnPaySignature — ký + verify HMAC-SHA512 với secret giả.
 * Không cần Spring context.
 */
@DisplayName("VnPaySignature")
class VnPaySignatureTest {

    private static final String SECRET = "FAKEHASHSECRET1234567890";

    private Map<String, String> sampleParams() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", "DEMO");
        p.put("vnp_Amount", "10000000");
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", "NAP4F8A2C1B3");
        p.put("vnp_OrderInfo", "Nap vi NAP4F8A2C1B3");
        p.put("vnp_Locale", "vn");
        return p;
    }

    @Test
    @DisplayName("buildSignedQuery → tạo query có vnp_SecureHash và verify lại thành công")
    void buildAndVerifyRoundTrip() {
        Map<String, String> params = sampleParams();
        String query = VnPaySignature.buildSignedQuery(params, SECRET);

        assertThat(query).contains("vnp_SecureHash=");

        // Parse lại query thành map (giả lập VNPay gửi về) rồi verify
        Map<String, String> received = parseQuery(query);
        assertThat(VnPaySignature.verify(received, SECRET)).isTrue();
    }

    @Test
    @DisplayName("verify thất bại khi đổi giá trị một field (giả mạo amount)")
    void verifyFailsWhenTampered() {
        Map<String, String> params = sampleParams();
        String query = VnPaySignature.buildSignedQuery(params, SECRET);
        Map<String, String> received = parseQuery(query);

        // Giả mạo: tăng amount nhưng giữ nguyên chữ ký cũ
        received.put("vnp_Amount", "99999999");

        assertThat(VnPaySignature.verify(received, SECRET)).isFalse();
    }

    @Test
    @DisplayName("verify thất bại khi sai secret")
    void verifyFailsWrongSecret() {
        Map<String, String> params = sampleParams();
        String query = VnPaySignature.buildSignedQuery(params, SECRET);
        Map<String, String> received = parseQuery(query);

        assertThat(VnPaySignature.verify(received, "WRONG_SECRET")).isFalse();
    }

    @Test
    @DisplayName("verify thất bại khi thiếu vnp_SecureHash")
    void verifyFailsMissingHash() {
        assertThat(VnPaySignature.verify(sampleParams(), SECRET)).isFalse();
        assertThat(VnPaySignature.verify(new HashMap<>(), SECRET)).isFalse();
    }

    @Test
    @DisplayName("verify bỏ qua vnp_SecureHashType khi tính lại chữ ký")
    void verifyIgnoresSecureHashType() {
        Map<String, String> params = sampleParams();
        String query = VnPaySignature.buildSignedQuery(params, SECRET);
        Map<String, String> received = parseQuery(query);
        received.put("vnp_SecureHashType", "HmacSHA512");

        assertThat(VnPaySignature.verify(received, SECRET)).isTrue();
    }

    /** Parse query "a=b&c=d" thành map, decode value. */
    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String key = pair.substring(0, idx);
            String value = java.net.URLDecoder.decode(
                    pair.substring(idx + 1), java.nio.charset.StandardCharsets.US_ASCII);
            map.put(key, value);
        }
        return map;
    }
}
