package com.example.learning_system_spring.infrastructure.payment;

import com.example.learning_system_spring.application.port.PaymentInitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho VnPayGateway — build URL thanh toán đã ký, displayType=REDIRECT_URL.
 */
@DisplayName("VnPayGateway")
class VnPayGatewayTest {

    private static final String SECRET = "FAKEHASHSECRET1234567890";
    private VnPayGateway gateway;

    @BeforeEach
    void setUp() {
        VnPayProperties props = new VnPayProperties();
        props.setTmnCode("DEMO");
        props.setHashSecret(SECRET);
        props.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        props.setReturnUrl("http://localhost:5173/wallet/vnpay-return");
        props.setTtlMinutes(15);
        gateway = new VnPayGateway(props);
    }

    @Test
    @DisplayName("providerName trả về VNPAY (khớp enum TxSource)")
    void providerName() {
        assertThat(gateway.providerName()).isEqualTo("VNPAY");
    }

    @Test
    @DisplayName("initPayment → displayType=REDIRECT_URL, URL chứa pay-url + chữ ký hợp lệ")
    void initPaymentBuildsSignedUrl() {
        PaymentInitResult result = gateway.initPayment("NAP4F8A2C1B3", new BigDecimal("100000"));

        assertThat(result.displayType()).isEqualTo("REDIRECT_URL");
        assertThat(result.referenceCode()).isEqualTo("NAP4F8A2C1B3");
        assertThat(result.amount()).isEqualByComparingTo("100000");
        assertThat(result.displayData())
                .startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?")
                .contains("vnp_TxnRef=NAP4F8A2C1B3")
                .contains("vnp_Amount=10000000")  // 100000 * 100
                .contains("vnp_SecureHash=");

        // Chữ ký trong URL phải verify được
        String query = result.displayData().substring(result.displayData().indexOf('?') + 1);
        Map<String, String> received = parseQuery(query);
        assertThat(VnPaySignature.verify(received, SECRET)).isTrue();
    }

    @Test
    @DisplayName("vnp_Amount = amount * 100 (đơn vị VNPay)")
    void amountTimes100() {
        PaymentInitResult result = gateway.initPayment("NAPX", new BigDecimal("50000"));
        String query = result.displayData();
        assertThat(query).contains("vnp_Amount=5000000");
    }

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
