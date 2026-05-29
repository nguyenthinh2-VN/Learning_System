package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.request.User.ChangePasswordRequest;
import com.example.learning_system_spring.adapter.dto.request.User.UpdateProfileRequest;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.adapter.dto.response.MyEnrollmentResponse;
import com.example.learning_system_spring.adapter.dto.response.TransactionItemResponse;
import com.example.learning_system_spring.adapter.dto.response.UserProfileResponse;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.User.MyEnrollmentOutput;
import com.example.learning_system_spring.application.dto.User.UploadAvatarInput;
import com.example.learning_system_spring.application.dto.User.UserProfileOutput;
import com.example.learning_system_spring.application.dto.Wallet.TransactionItemOutput;
import com.example.learning_system_spring.application.usecase.User.ChangeMyPasswordUseCase;
import com.example.learning_system_spring.application.usecase.User.GetMyEnrollmentsUseCase;
import com.example.learning_system_spring.application.usecase.User.GetMyProfileUseCase;
import com.example.learning_system_spring.application.usecase.User.TopUpBalanceUseCase;
import com.example.learning_system_spring.application.usecase.User.UpdateMyProfileUseCase;
import com.example.learning_system_spring.application.usecase.User.UploadMyAvatarUseCase;
import com.example.learning_system_spring.application.usecase.Wallet.GetMyTransactionsUseCase;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final TopUpBalanceUseCase topUpBalanceUseCase;
    private final GetMyEnrollmentsUseCase getMyEnrollmentsUseCase;
    private final GetMyProfileUseCase getMyProfileUseCase;
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final ChangeMyPasswordUseCase changeMyPasswordUseCase;
    private final UploadMyAvatarUseCase uploadMyAvatarUseCase;
    private final GetMyTransactionsUseCase getMyTransactionsUseCase;
    private final JwtService jwtService;

    private Claims getClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }

    private Long getUserId(HttpServletRequest request) {
        return getClaims(request).get("userId", Long.class);
    }

    /**
     * Xem thông tin cá nhân + số dư ví của chính mình.
     * FE dùng endpoint này để hiển thị balance trên header/navbar.
     *
     * GET /api/v1/users/me/profile
     */
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(HttpServletRequest request) {
        UserProfileOutput output = getMyProfileUseCase.execute(getUserId(request));
        return ResponseEntity.ok(ApiResponse.success(UserProfileResponse.from(output)));
    }

    /**
     * Cập nhật thông tin cá nhân (name, avatarUrl) của chính mình.
     * avatarUrl: null = giữ nguyên, "" = xóa avatar.
     *
     * PUT /api/v1/users/me/profile
     */
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest req,
            HttpServletRequest request) {

        UserProfileOutput output = updateMyProfileUseCase.execute(req.toInput(getUserId(request)));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công",
                UserProfileResponse.from(output)));
    }

    /**
     * Đổi mật khẩu của chính mình. Yêu cầu mật khẩu hiện tại.
     *
     * PUT /api/v1/users/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest req,
            HttpServletRequest request) {

        changeMyPasswordUseCase.execute(req.toInput(getUserId(request)));
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }

    /**
     * Upload ảnh avatar (multipart). Lưu trên ổ đĩa BE, tự gán avatarUrl.
     * Chỉ chấp nhận JPEG/PNG/WebP, tối đa 2MB.
     *
     * POST /api/v1/users/me/avatar  (form-data, part "file")
     */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadMyAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc file upload: " + e.getMessage(), e);
        }

        UploadAvatarInput input = new UploadAvatarInput(getUserId(request), content, file.getContentType());
        UserProfileOutput output = uploadMyAvatarUseCase.execute(input);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh đại diện thành công",
                UserProfileResponse.from(output)));
    }

    /**
     * Nạp tiền trực tiếp vào ví (legacy — không qua payment gateway).
     *
     * POST /api/v1/users/me/top-up
     */
    @PostMapping("/me/top-up")
    public ResponseEntity<?> topUp(
            @RequestBody Map<String, BigDecimal> requestBody,
            HttpServletRequest request) {

        Long requesterId = getUserId(request);

        BigDecimal amount = requestBody.get("amount");
        BigDecimal newBalance = topUpBalanceUseCase.execute(requesterId, amount);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Nạp tiền thành công",
                "data", Map.of("newBalance", newBalance),
                "timestamp", LocalDateTime.now()));
    }

    /**
     * Lấy danh sách khóa học đã mua của chính người dùng đang đăng nhập.
     * Trả về trang rỗng nếu chưa có enrollment nào.
     *
     * GET /api/v1/users/me/enrollments?page=0&size=20
     */
    @GetMapping("/me/enrollments")
    public ResponseEntity<ApiResponse<PageResult<MyEnrollmentResponse>>> getMyEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        Long requesterId = getUserId(request);

        PageResult<MyEnrollmentOutput> result = getMyEnrollmentsUseCase.execute(requesterId, page, size);
        PageResult<MyEnrollmentResponse> response = result.map(MyEnrollmentResponse::from);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Lấy lịch sử giao dịch ví của chính người dùng đang đăng nhập (phân trang, mới nhất trước).
     * Bao gồm cả tiền vào (nạp tiền) và tiền ra (mua khóa học).
     *
     * GET /api/v1/users/me/transactions?page=0&size=20
     */
    @GetMapping("/me/transactions")
    public ResponseEntity<ApiResponse<PageResult<TransactionItemResponse>>> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        Long requesterId = getUserId(request);

        PageResult<TransactionItemOutput> result = getMyTransactionsUseCase.execute(requesterId, page, size);
        PageResult<TransactionItemResponse> response = result.map(TransactionItemResponse::from);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
