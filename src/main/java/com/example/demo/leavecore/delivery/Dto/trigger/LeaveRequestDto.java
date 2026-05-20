package com.example.demo.leavecore.delivery.Dto.trigger;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object (DTO) đại diện cho yêu cầu xin nghỉ phép gửi từ giao diện người dùng.
 * Áp dụng các tiêu chuẩn Jakarta Bean Validation (JSR-380) đồng bộ với Camunda Form.
 * Sử dụng Jackson annotations để bảo toàn 100% tên biến tương thích với Job Worker hiện tại.
 */
@Data
public class LeaveRequestDto {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @JsonProperty("Email")
    private String Email;

    @NotBlank(message = "Mã nhân viên không được để trống")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
             message = "Mã nhân viên phải là định dạng UUID hợp lệ")
    @JsonProperty("EmployeeID")
    private String EmployeeID;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự")
    @JsonProperty("FullName")
    private String FullName;

    @NotBlank(message = "Phòng ban không được để trống")
    @Pattern(regexp = "^(IT|HR)$", message = "Phòng ban phải là IT hoặc HR")
    @JsonProperty("Department")
    private String Department;

    @NotEmpty(message = "Loại nghỉ phép không được để trống")
    @JsonProperty("LeaveType")
    private List<@Pattern(regexp = "^(ANNUAL|SICK|UNPAID)$", message = "Loại phép phải là ANNUAL, SICK hoặc UNPAID") String> LeaveType;

    @NotBlank(message = "Ngày bắt đầu không được để trống")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Ngày bắt đầu phải có định dạng yyyy-MM-dd")
    @JsonProperty("StartDate")
    private String StartDate;

    @NotBlank(message = "Ngày kết thúc không được để trống")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Ngày kết thúc phải có định dạng yyyy-MM-dd")
    @JsonProperty("EndDate")
    private String EndDate;

    @NotBlank(message = "Ca nghỉ không được để trống")
    @Pattern(regexp = "^(MORNING|AFTERNOON|ALLDAY|ALL_DAY)$", message = "Ca nghỉ phải là MORNING, AFTERNOON, ALLDAY hoặc ALL_DAY")
    @JsonProperty("leaveSession")
    private String leaveSession;

    @NotNull(message = "Tổng số ngày phép không được để trống")
    @DecimalMin(value = "0.5", message = "Tổng số ngày phép phải lớn hơn hoặc bằng 0.5")
    @JsonProperty("totaldays")
    private Double totaldays;

    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    @JsonProperty("Reason")
    @JsonAlias("reason")
    private String reason;

    @JsonProperty("Status")
    private String Status;

    @JsonProperty("BusinessKey")
    private String businessKey;

    /**
     * Tự động chuẩn hóa ca nghỉ từ ALLDAY (giá trị trong form UI) thành ALL_DAY (tên enum trong Java)
     * khi Jackson thực hiện serialize DTO gửi sang Zeebe Broker.
     */
    @JsonProperty("leaveSession")
    public String getLeaveSession() {
        if ("ALLDAY".equalsIgnoreCase(this.leaveSession)) {
            return "ALL_DAY";
        }
        return this.leaveSession;
    }

    /**
     * Cung cấp thêm thuộc tính TotalWorkingDays tự động ánh xạ từ totaldays để đồng bộ hoàn hảo
     * với cả hai dạng biến mà Job Worker có thể bóc tách (cả totaldays và TotalWorkingDays).
     */
    @JsonProperty("TotalWorkingDays")
    public Double getTotalWorkingDays() {
        return this.totaldays;
    }

    /**
     * Tự động sinh BusinessKey tạm thời nếu không được truyền từ client
     * để tránh lỗi kiểm tra NotBlank trong cấu trúc dữ liệu của Zeebe Job Worker.
     */
    public String getBusinessKey() {
        if (this.businessKey == null || this.businessKey.trim().isEmpty()) {
            return "LR-TEMP-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return this.businessKey;
    }
}
