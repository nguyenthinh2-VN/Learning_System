package com.example.learning_system_spring.application.port;

import java.math.BigDecimal;

/**
 * Port interface cho payment gateway.
 * KHÔNG BAO GIỜ sửa interface này khi thêm provider mới.
 * Chỉ thêm implementation mới (VietQrGateway, MomoGateway, v.v.)
 */
public interface PaymentGateway {

    /**
     * Khởi tạo yêu cầu thanh toán.
     *
     * @param referenceCode mã tham chiếu duy nhất đã được tạo sẵn
     * @param amount        số tiền cần thanh toán
     * @return thông tin để FE hiển thị (QR URL, message hướng dẫn, v.v.)
     */
    PaymentInitResult initPayment(String referenceCode, BigDecimal amount);

    /**
     * Tên provider — ghi vào wallet_transactions.source.
     * Phải khớp với giá trị trong enum TxSource.
     */
    String providerName();
}
