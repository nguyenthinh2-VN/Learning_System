package com.example.learning_system_spring.adapter.dto.request.Course;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body cho POST /api/v1/courses/{id}/purchase và POST /api/v1/courses/{id}/quote.
 *
 * Anti-tampering: DTO CHỈ khai báo voucherCode. Tuyệt đối KHÔNG khai báo bất kỳ field
 * giá tiền nào (price, originalPrice, discount, finalPrice, paidPrice). Spring sẽ KHÔNG
 * bind các field thừa từ client (nếu có) vào DTO này — server hoàn toàn bỏ qua.
 */
@Getter
@Setter
@NoArgsConstructor
public class PurchaseCourseRequest {

    @Size(max = 32, message = "Mã voucher tối đa 32 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "Mã voucher chỉ chứa A-Z, a-z, 0-9, _, -")
    private String voucherCode;
}
