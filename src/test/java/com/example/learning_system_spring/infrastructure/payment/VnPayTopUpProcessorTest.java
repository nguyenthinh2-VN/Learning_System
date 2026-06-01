package com.example.learning_system_spring.infrastructure.payment;

import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.application.usecase.Wallet.CompleteTopUpUseCase;
import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import com.example.learning_system_spring.infrastructure.service.WalletNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test cho VnPayTopUpProcessor — logic dùng chung cho kênh RETURN và IPN.
 */
@DisplayName("VnPayTopUpProcessor")
class VnPayTopUpProcessorTest {

    private static final String SECRET = "FAKEHASHSECRET1234567890";
    private static final String REF = "NAP4F8A2C1B3";
    private static final Long USER_ID = 1L;

    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private CompleteTopUpUseCase completeTopUpUseCase;
    @Mock private WalletNotificationService walletNotificationService;

    private VnPayProperties props;
    private VnPayTopUpProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        props = new VnPayProperties();
        props.setHashSecret(SECRET);
        props.setCompleteMode("RETURN");
        processor = new VnPayTopUpProcessor(
                walletTransactionRepository, completeTopUpUseCase, walletNotificationService, props);
    }

    /** Build params có chữ ký hợp lệ với responseCode + amount cho trước. */
    private Map<String, String> signedParams(String responseCode, long vnpAmount) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("vnp_TxnRef", REF);
        p.put("vnp_Amount", String.valueOf(vnpAmount));
        p.put("vnp_ResponseCode", responseCode);
        p.put("vnp_TransactionNo", "987654");
        String query = VnPaySignature.buildSignedQuery(p, SECRET);
        return parseQuery(query);
    }

    private WalletTransaction pendingTx(BigDecimal amount) {
        return WalletTransaction.createPending(USER_ID, amount, TxSource.VNPAY, 15);
    }

    private WalletTransaction completedTx(BigDecimal amount) {
        return WalletTransaction.reconstitute(
                10L, USER_ID, REF, amount, TxStatus.COMPLETED, TxSource.VNPAY,
                "vnpay", java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now());
    }

    @Test
    @DisplayName("sai chữ ký → INVALID_SIGNATURE, không cộng tiền")
    void invalidSignature() {
        Map<String, String> params = signedParams("00", 10000000L);
        params.put("vnp_Amount", "99999999"); // giả mạo sau khi ký

        VnPayTopUpProcessor.Result result = processor.processReturn(params, USER_ID);

        assertThat(result.outcome()).isEqualTo(VnPayTopUpProcessor.Outcome.INVALID_SIGNATURE);
        verify(completeTopUpUseCase, never()).execute(anyString(), anyString());
    }

    @Test
    @DisplayName("không tìm thấy tx → TX_NOT_FOUND")
    void txNotFound() {
        Map<String, String> params = signedParams("00", 10000000L);
        when(walletTransactionRepository.findByReferenceCode(REF)).thenReturn(Optional.empty());

        VnPayTopUpProcessor.Result result = processor.processReturn(params, USER_ID);

        assertThat(result.outcome()).isEqualTo(VnPayTopUpProcessor.Outcome.TX_NOT_FOUND);
        verify(completeTopUpUseCase, never()).execute(anyString(), anyString());
    }

    @Test
    @DisplayName("RETURN: user khác chủ giao dịch → TX_NOT_FOUND (không lộ giao dịch)")
    void notOwner() {
        Map<String, String> params = signedParams("00", 10000000L);
        when(walletTransactionRepository.findByReferenceCode(REF))
                .thenReturn(Optional.of(pendingTx(new BigDecimal("100000"))));

        VnPayTopUpProcessor.Result result = processor.processReturn(params, 999L);

        assertThat(result.outcome()).isEqualTo(VnPayTopUpProcessor.Outcome.TX_NOT_FOUND);
        verify(completeTopUpUseCase, never()).execute(anyString(), anyString());
    }

    @Test
    @DisplayName("amount lệch → AMOUNT_MISMATCH, không cộng tiền")
    void amountMismatch() {
        // tx.amount = 100000 nhưng vnp_Amount = 50000*100
        Map<String, String> params = signedParams("00", 5000000L);
        when(walletTransactionRepository.findByReferenceCode(REF))
                .thenReturn(Optional.of(pendingTx(new BigDecimal("100000"))));

        VnPayTopUpProcessor.Result result = processor.processReturn(params, USER_ID);

        assertThat(result.outcome()).isEqualTo(VnPayTopUpProcessor.Outcome.AMOUNT_MISMATCH);
        verify(completeTopUpUseCase, never()).execute(anyString(), anyString());
    }

    @Test
    @DisplayName("happy path RETURN → SUCCESS, gọi complete + push WS")
    void successReturn() {
        Map<String, String> params = signedParams("00", 10000000L);
        when(walletTransactionRepository.findByReferenceCode(REF))
                .thenReturn(Optional.of(pendingTx(new BigDecimal("100000"))));
        when(completeTopUpUseCase.execute(eq(REF), anyString()))
                .thenReturn(new CompleteTopUpUseCase.Result(
                        "MEM2B4A1D", USER_ID, new BigDecimal("600000"),
                        new BigDecimal("100000"), "VNPAY", REF, "vnpay:987654"));

        VnPayTopUpProcessor.Result result = processor.processReturn(params, USER_ID);

        assertThat(result.outcome()).isEqualTo(VnPayTopUpProcessor.Outcome.SUCCESS);
        assertThat(result.amount()).isEqualByComparingTo("100000");
        verify(completeTopUpUseCase, times(1)).execute(eq(REF), anyString());
        verify(walletNotificationService, times(1)).pushWalletUpdated(
                eq("MEM2B4A1D"), eq(USER_ID), any(), any(), eq("VNPAY"), eq(REF), anyString());
    }

    @Test
    @DisplayName("tx đã COMPLETED → ALREADY_COMPLETED (idempotent), không gọi complete")
    void alreadyCompleted() {
        Map<String, String> params = signedParams("00", 10000000L);
        when(walletTransactionRepository.findByReferenceCode(REF))
                .thenReturn(Optional.of(completedTx(new BigDecimal("100000"))));

        VnPayTopUpProcessor.Result result = processor.processReturn(params, USER_ID);

        assertThat(result.outcome()).isEqualTo(VnPayTopUpProcessor.Outcome.ALREADY_COMPLETED);
        verify(completeTopUpUseCase, never()).execute(anyString(), anyString());
    }

    @Test
    @DisplayName("vnp_ResponseCode != 00 → PAYMENT_FAILED, không cộng tiền")
    void paymentFailed() {
        Map<String, String> params = signedParams("24", 10000000L); // 24 = user huỷ
        when(walletTransactionRepository.findByReferenceCode(REF))
                .thenReturn(Optional.of(pendingTx(new BigDecimal("100000"))));

        VnPayTopUpProcessor.Result result = processor.processReturn(params, USER_ID);

        assertThat(result.outcome()).isEqualTo(VnPayTopUpProcessor.Outcome.PAYMENT_FAILED);
        verify(completeTopUpUseCase, never()).execute(anyString(), anyString());
    }

    @Test
    @DisplayName("complete-mode=IPN: kênh RETURN chỉ ACKNOWLEDGED, không cộng tiền")
    void returnNotAllowedInIpnMode() {
        props.setCompleteMode("IPN");
        Map<String, String> params = signedParams("00", 10000000L);
        when(walletTransactionRepository.findByReferenceCode(REF))
                .thenReturn(Optional.of(pendingTx(new BigDecimal("100000"))));

        VnPayTopUpProcessor.Result result = processor.processReturn(params, USER_ID);

        assertThat(result.outcome()).isEqualTo(VnPayTopUpProcessor.Outcome.ACKNOWLEDGED);
        verify(completeTopUpUseCase, never()).execute(anyString(), anyString());
    }

    @Test
    @DisplayName("complete-mode=IPN: kênh IPN cộng tiền thành công")
    void ipnSuccess() {
        props.setCompleteMode("IPN");
        Map<String, String> params = signedParams("00", 10000000L);
        when(walletTransactionRepository.findByReferenceCode(REF))
                .thenReturn(Optional.of(pendingTx(new BigDecimal("100000"))));
        when(completeTopUpUseCase.execute(eq(REF), anyString()))
                .thenReturn(new CompleteTopUpUseCase.Result(
                        "MEM2B4A1D", USER_ID, new BigDecimal("600000"),
                        new BigDecimal("100000"), "VNPAY", REF, "vnpay:987654"));

        VnPayTopUpProcessor.Result result = processor.processIpn(params);

        assertThat(result.outcome()).isEqualTo(VnPayTopUpProcessor.Outcome.SUCCESS);
        verify(completeTopUpUseCase, times(1)).execute(eq(REF), anyString());
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
