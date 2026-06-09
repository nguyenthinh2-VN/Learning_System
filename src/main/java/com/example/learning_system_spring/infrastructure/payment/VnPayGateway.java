package com.example.learning_system_spring.infrastructure.payment;

import com.example.learning_system_spring.application.port.PaymentGateway;
import com.example.learning_system_spring.application.port.PaymentInitResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Payment gateway VNPay — chỉ active khi payment.provider=vnpay.
 *
 * Build URL thanh toán đã ký HMAC-SHA512, trả displayType="REDIRECT_URL" để FE mở tab mới.
 * KHÔNG sửa interface PaymentGateway hay use case cũ (đúng Open/Closed).
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "vnpay")
public class VnPayGateway implements PaymentGateway {

    /** displayType quy ước để FE biết phải mở tab redirect thay vì render ảnh QR. */
    public static final String DISPLAY_REDIRECT_URL = "REDIRECT_URL";

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayProperties props;

    public VnPayGateway(VnPayProperties props) {
        this.props = props;
    }

    @Override
    public PaymentInitResult initPayment(String referenceCode, BigDecimal amount) {
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        ZonedDateTime expire = now.plusMinutes(props.getTtlMinutes());

        // vnp_Amount: VNPay yêu cầu số tiền * 100, kiểu long, không phần thập phân.
        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", props.getTmnCode());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", referenceCode);
        params.put("vnp_OrderInfo", "Nap vi " + referenceCode);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", props.getReturnUrl());
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", now.format(FMT));
        params.put("vnp_ExpireDate", expire.format(FMT));

        String signedQuery = VnPaySignature.buildSignedQuery(params, props.getHashSecret());
        String url = props.getPayUrl() + "?" + signedQuery;

        return new PaymentInitResult(
                referenceCode,
                amount,
                url,
                DISPLAY_REDIRECT_URL,
                expire.toLocalDateTime()
        );
    }

    @Override
    public String providerName() {
        return "VNPAY";
    }
}
