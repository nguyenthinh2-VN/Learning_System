package com.example.learning_system_spring.domain.model;

import com.example.learning_system_spring.domain.exception.CourseAlreadyPublishedException;
import com.example.learning_system_spring.domain.exception.CoursePriceLockedException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Course {
    private Long id;
    private String title;
    private String description;
    private int maxStudents;
    private int enrolledCount;
    private BigDecimal price;
    private Long instructorId;
    private String thumbnailUrl;
    private boolean published;
    private boolean priceLocked;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    /** Nếu true: thành viên nội bộ (user.isInternal) được học miễn phí khóa này. */
    private boolean freeForInternal;
    private boolean isMandatory;
    private String assignedDepartment;
    private LocalDateTime mandatoryDeadline;
    @Builder.Default
    private List<CourseSection> sections = new ArrayList<>();

    /**
     * Tạo course mới. Mặc định ẨN (published = false, priceLocked = false).
     */
    public static Course create(String title, String description, int maxStudents, BigDecimal price,
            Long instructorId, String thumbnailUrl, boolean freeForInternal, List<CourseSection> sections) {
        if (maxStudents <= 0) {
            throw new IllegalArgumentException("Max students must be greater than 0");
        }
        return Course.builder()
                .title(title)
                .description(description)
                .maxStudents(maxStudents)
                .price(price != null ? price : BigDecimal.ZERO)
                .enrolledCount(0)
                .instructorId(instructorId)
                .thumbnailUrl(thumbnailUrl)
                .published(false)
                .priceLocked(false)
                .freeForInternal(freeForInternal)
                .publishedAt(null)
                .publishedBy(null)
                .sections(sections != null ? new ArrayList<>(sections) : new ArrayList<>())
                .build();
    }

    /**
     * Tạo course mới kèm thiết lập bắt buộc.
     */
    public static Course create(String title, String description, int maxStudents, BigDecimal price,
            Long instructorId, String thumbnailUrl, boolean freeForInternal, boolean isMandatory,
            String assignedDepartment, LocalDateTime mandatoryDeadline, List<CourseSection> sections) {
        Course course = create(title, description, maxStudents, price, instructorId, thumbnailUrl, freeForInternal, sections);
        course.isMandatory = isMandatory;
        course.assignedDepartment = assignedDepartment;
        course.mandatoryDeadline = mandatoryDeadline;
        return course;
    }

    /** Overload tương thích ngược — freeForInternal mặc định false. */
    public static Course create(String title, String description, int maxStudents, BigDecimal price,
            Long instructorId, String thumbnailUrl, List<CourseSection> sections) {
        return create(title, description, maxStudents, price, instructorId, thumbnailUrl, false, sections);
    }

    public static Course reconstitute(Long id, String title, String description, int maxStudents, int enrolledCount,
            BigDecimal price, Long instructorId, String thumbnailUrl, boolean published, boolean priceLocked,
            boolean freeForInternal, LocalDateTime publishedAt, Long publishedBy, List<CourseSection> sections) {
        return Course.builder()
                .id(id)
                .title(title)
                .description(description)
                .maxStudents(maxStudents)
                .price(price != null ? price : BigDecimal.ZERO)
                .enrolledCount(enrolledCount)
                .instructorId(instructorId)
                .thumbnailUrl(thumbnailUrl)
                .published(published)
                .priceLocked(priceLocked)
                .freeForInternal(freeForInternal)
                .publishedAt(publishedAt)
                .publishedBy(publishedBy)
                .sections(sections != null ? new ArrayList<>(sections) : new ArrayList<>())
                .build();
    }

    /** Overload cho các trường bắt buộc. */
    public static Course reconstitute(Long id, String title, String description, int maxStudents, int enrolledCount,
            BigDecimal price, Long instructorId, String thumbnailUrl, boolean published, boolean priceLocked,
            boolean freeForInternal, boolean isMandatory, String assignedDepartment, LocalDateTime mandatoryDeadline,
            LocalDateTime publishedAt, Long publishedBy, List<CourseSection> sections) {
        Course course = reconstitute(id, title, description, maxStudents, enrolledCount, price, instructorId, thumbnailUrl,
                published, priceLocked, freeForInternal, publishedAt, publishedBy, sections);
        course.isMandatory = isMandatory;
        course.assignedDepartment = assignedDepartment;
        course.mandatoryDeadline = mandatoryDeadline;
        return course;
    }

    /** Overload tương thích ngược — freeForInternal mặc định false. */
    public static Course reconstitute(Long id, String title, String description, int maxStudents, int enrolledCount,
            BigDecimal price, Long instructorId, String thumbnailUrl, boolean published, boolean priceLocked,
            LocalDateTime publishedAt, Long publishedBy, List<CourseSection> sections) {
        return reconstitute(id, title, description, maxStudents, enrolledCount, price, instructorId, thumbnailUrl,
                published, priceLocked, false, publishedAt, publishedBy, sections);
    }

    public boolean isFull() {
        return enrolledCount >= maxStudents;
    }

    public void enroll() {
        if (isFull()) {
            throw new IllegalStateException("Course is already full");
        }
        enrolledCount++;
    }

    /**
     * Duyệt và publish course. Tự động khóa giá để INSTRUCTOR không sửa được.
     * Nếu cần sửa giá sau publish, admin phải dùng UpdateCoursePriceUseCase.
     */
    public void publish(Long publisherId) {
        if (this.published) {
            throw new CourseAlreadyPublishedException(this.id);
        }
        if (this.price == null || this.price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Giá khóa học không hợp lệ. Vui lòng đặt giá trước khi publish.");
        }
        this.published = true;
        this.priceLocked = true;
        this.publishedAt = LocalDateTime.now();
        this.publishedBy = publisherId;
    }

    /**
     * Ẩn course đã publish khỏi public listing. Giữ nguyên priceLocked để khi
     * publish lại không cần đặt giá lại.
     */
    public void unpublish() {
        this.published = false;
    }

    /**
     * Cập nhật giá. Nếu priceLocked = true (sau publish), chỉ admin (isAdmin = true)
     * mới được sửa. INSTRUCTOR khi đó phải nhờ admin.
     */
    public void updatePrice(BigDecimal newPrice, boolean isAdmin) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá khóa học phải >= 0");
        }
        if (this.priceLocked && !isAdmin) {
            throw new CoursePriceLockedException(this.id);
        }
        this.price = newPrice;
    }

    public void updateMandatoryConfig(boolean isMandatory, String assignedDepartment, LocalDateTime mandatoryDeadline) {
        this.isMandatory = isMandatory;
        this.assignedDepartment = assignedDepartment;
        this.mandatoryDeadline = mandatoryDeadline;
    }
}
