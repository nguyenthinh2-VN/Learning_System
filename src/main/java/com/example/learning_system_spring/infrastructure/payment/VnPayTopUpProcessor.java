package com.example.learning_system_spring.infrastructure.payment;

import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.application.usecase.Wallet.CompleteTopUpUseCase;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import com.example.learning_system_spring.infrastructure.service.WalletNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Xử lý kết quả thanh toán VNPay dùng chung cho 2 kênh: RETURN (dev) và IPN (production).
 *
 * Mọi lần hoàn tất đều:
 *  1. Verify chữ ký HMAC-SHA512 (server-side).
 *  2. Tra giao dịch theo vnp_TxnRef.
 *  3. Đối soát số tiền: vnp_Amount/100 phải khớp tx.amount nội bộ.
 *  4. Chỉ cộng đúng tx.getAmount() (không tin amount từ query).
 *  5. Idempotent: tx đã COMPLETED → không cộng lại.
 *
 * Chỉ active khi payment.provider=vnpay.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "payment.provider", havingValue = "vnpay")
public class VnPayTopUpProcessor {

    private final WalletTransactionRepository walletTransactionRepository;
    private final CompleteTopUpUseCase completeTopUpUseCase;
    private final WalletNotificationService walletNotificationService;
    private final VnPayProperties props;

    public VnPayTopUpProcessor(WalletTransactionRepository walletTransactionRepository,
                               CompleteTopUpUseCase completeTopUpUseCase,
                               WalletNotificationService walletNotificationService,
                               VnPayProperties props) {
        this.walletTransactionRepository = walletTransactionRepository;
        this.completeTopUpUseCase = completeTopUpUseCase;
        this.walletNotificationService = walletNotificationService;
        this.props = props;
    }

    public enum Outcome {
        SUCCESS,            // vừa cộng tiền thành công
        ALREADY_COMPLETED,  // đã COMPLETED từ trước (idempotent)
        ACKNOWLEDGED,       // verify ok nhưng kênh này không được phép cộng (chờ kênh kia)
        PAYMENT_FAILED,     // vnp_ResponseCode != "00" (user huỷ/thất bại)
        INVALID_SIGNATURE,
        TX_NOT_FOUND,
        AMOUNT_MISMATCH
    }

    public record Result(Outcome outcome, String referenceCode, String status, BigDecimal amount) {}

    /**
     * Kênh RETURN — gọi từ trang FE sau khi user quay lại. Có kiểm tra chủ sở hữu giao dịch.
     *
     * @param params           toàn bộ vnp_* (gồm vnp_SecureHash)
     * @param requesterUserId  userId từ JWT (chống complete hộ người khác)
     */
    public Result processReturn(Map<String, String> params, Long requesterUserId) {
        return process(params, requesterUserId, props.isReturnMode());
    }

    /**
     * Kênh IPN — VNPay gọi server→server, không có JWT nên không kiểm tra chủ sở hữu.
     */
    public Result processIpn(Map<String, String> params) {
        return process(params, null, props.isIpnMode());
    }

    /**
     * @param completeAllowed kênh hiện tại có được phép cộng tiền không (theo complete-mode).
     *                        Kênh không được chọn chỉ verify + trả ACKNOWLEDGED để tránh cộng 2 lần.
     */
    private Result process(Map<String, String> params, Long requesterUserId, boolean completeAllowed) {
        // 1. Verify chữ ký
        if (!VnPaySignature.verify(params, props.getHashSecret())) {
            log.warn("[VNPay] Sai chữ ký cho ref={}", params.get("vnp_TxnRef"));
            return new Result(Outcome.INVALID_SIGNATURE, params.get("vnp_TxnRef"), null, null);
        }

        String ref = params.get("vnp_TxnRef");
        Optional<WalletTransaction> opt = walletTransactionRepository.findByReferenceCode(ref);
        if (opt.isEmpty()) {
            log.warn("[VNPay] Không tìm thấy giao dịch ref={}", ref);
            return new Result(Outcome.TX_NOT_FOUND, ref, null, null);
        }
        WalletTransaction tx = opt.get();

        // Kênh RETURN: chỉ chủ giao dịch mới được hoàn tất.
        if (requesterUserId != null && !tx.getUserId().equals(requesterUserId)) {
            log.warn("[VNPay] User {} cố hoàn tất giao dịch ref={} không thuộc sở hữu",
                    requesterUserId, ref);
            return new Result(Outcome.TX_NOT_FOUND, ref, null, null);
        }

        // 3. Đối soát số tiền — vnp_Amount đơn vị *100
        if (!amountMatches(params.get("vnp_Amount"), tx.getAmount())) {
            log.error("[VNPay] amount_mismatch ref={} vnp_Amount={} tx.amount={}",
                    ref, params.get("vnp_Amount"), tx.getAmount());
            return new Result(Outcome.AMOUNT_MISMATCH, ref, tx.getStatus().name(), tx.getAmount());
        }

        // Đã hoàn tất từ trước → idempotent, không cộng lại.
        if (tx.getStatus() == TxStatus.COMPLETED) {
            return new Result(Outcome.ALREADY_COMPLETED, ref, TxStatus.COMPLETED.name(), tx.getAmount());
        }

        // 4. Kiểm tra kết quả thanh toán
        String responseCode = params.get("vnp_ResponseCode");
        if (!"00".equals(responseCode)) {
            log.info("[VNPay] Thanh toán thất bại ref={} responseCode={}", ref, responseCode);
            return new Result(Outcome.PAYMENT_FAILED, ref, tx.getStatus().name(), tx.getAmount());
        }

        // Kênh này không được phép cộng (complete-mode khác) → chỉ ghi nhận.
        if (!completeAllowed) {
            return new Result(Outcome.ACKNOWLEDGED, ref, tx.getStatus().name(), tx.getAmount());
        }

        // 5. Hoàn tất + push WebSocket. CompleteTopUpUseCase đã có pessimistic lock + idempotent.
        try {
            CompleteTopUpUseCase.Result result = completeTopUpUseCase.execute(
                    ref, "vnpay:" + params.getOrDefault("vnp_TransactionNo", ""));
            walletNotificationService.pushWalletUpdated(
                    result.username(), result.userId(), result.newBalance(),
                    result.addedAmount(), result.source(), result.referenceCode(), result.note());
            return new Result(Outcome.SUCCESS, ref, TxStatus.COMPLETED.name(), result.addedAmount());
        } catch (IllegalStateException e) {
            // Race: kênh kia vừa complete xong giữa chừng → coi như đã hoàn tất.
            log.info("[VNPay] Hoàn tất ref={} bị chặn bởi state: {}", ref, e.getMessage());
            WalletTransaction latest = walletTransactionRepository.findByReferenceCode(ref).orElse(tx);
            if (latest.getStatus() == TxStatus.COMPLETED) {
                return new Result(Outcome.ALREADY_COMPLETED, ref, TxStatus.COMPLETED.name(), latest.getAmount());
            }
            return new Result(Outcome.PAYMENT_FAILED, ref, latest.getStatus().name(), latest.getAmount());
        }
    }

    private boolean amountMatches(String vnpAmountRaw, BigDecimal txAmount) {
        if (vnpAmountRaw == null) return false;
        try {
            BigDecimal vnpAmount = new BigDecimal(vnpAmountRaw)
                    .divide(BigDecimal.valueOf(100));
            return vnpAmount.compareTo(txAmount) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
