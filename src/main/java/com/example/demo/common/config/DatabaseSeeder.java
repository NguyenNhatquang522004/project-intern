package com.example.demo.common.config;

import com.example.demo.common.Enum.*;
import com.example.demo.leavecore.delivery.Dto.auth.RegisterStep1Request;
import com.example.demo.leavecore.domain.IRepository.IRepositoryUser;
import com.example.demo.leavecore.domain.entity.*;
import com.example.demo.leavecore.infrastructure.postgres.Repository.*;
import com.example.demo.leavecore.utils.PasswordUtils;
import com.example.demo.leaveinfrastructure.domain.entity.ApprovalHistory;
import com.example.demo.leaveinfrastructure.infrastructure.postgres.Repository.ApprovalHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DatabaseSeeder — chạy sau khi Hibernate khởi tạo schema (ddl-auto: update).
 * Idempotent: kiểm tra dữ liệu tồn tại trước khi insert.
 * Không chạy ở profile "production".
 */
@Slf4j
@Component
@Profile("!production")
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {
        private final IRepositoryUser userRepo;
        private final DepartmentRepository departmentRepository;
        private final EmployeeRepository employeeRepository;
        private final LeaveTypeRepository leaveTypeRepository;
        private final HolidayRepository holidayRepository;
        private final LeaveBalanceRepository leaveBalanceRepository;
        private final LeaveRequestRepository leaveRequestRepository;
        private final ApprovalHistoryRepository approvalHistoryRepository;
        private final PasswordUtils passwordUtils;

        @Override
        @Transactional
        public void run(ApplicationArguments args) {
                List<Employee> datalist = employeeRepository.findAll();
                if (datalist.size() > 0) {
                        return;
                }
                String passwordhash = passwordUtils.encodePassword("123");
                RegisterStep1Request user1 = RegisterStep1Request.builder()
                                .email("phuquy23031996@gmail.com")
                                .password("123")
                                .fullName(" Nguyễn Quý Phú")
                                .groupID("50010a19-c65f-452d-9785-92bdd8bf60e9")
                                .build();
                userRepo.CreateUser(user1);
                userRepo.UpdateIsActiveUser(user1.getEmail(), true);
                RegisterStep1Request user2 = RegisterStep1Request.builder()
                                .email("abc@gmail.com")
                                .password("123")
                                .fullName(" Nguyễn Văn An")
                                .groupID("9decc222-4f3a-44a8-91ea-534a8f1dcd6c")
                                .build();
                userRepo.CreateUser(user2);
                userRepo.UpdateIsActiveUser(user2.getEmail(), true);
                Employee employee1 = Employee.builder()
                                .email("phuquy23031996@gmail.com")
                                .fullName(" Nguyễn Quý Phú")
                                .password(passwordhash)
                                .status(AccountStatusEnum.ACTIVE)
                                .build();

                employeeRepository.save(employee1);

                Employee employee2 = Employee.builder()
                                .email("abc@gmail.com")
                                .fullName(" Nguyễn Văn An")
                                .password(passwordhash)
                                .status(AccountStatusEnum.ACTIVE)
                                .build();
                employeeRepository.save(employee2);

                List<Holiday> holidays = List.of(
                                Holiday.builder().holidayDate(LocalDate.of(2026, 1, 1))
                                                .description("Tết Dương lịch").build(),
                                Holiday.builder().holidayDate(LocalDate.of(2026, 1, 28))
                                                .description("Tết Nguyên Đán — Giao thừa").build(),
                                Holiday.builder().holidayDate(LocalDate.of(2026, 1, 29))
                                                .description("Tết Nguyên Đán — Mùng 1").build(),
                                Holiday.builder().holidayDate(LocalDate.of(2026, 1, 30))
                                                .description("Tết Nguyên Đán — Mùng 2").build(),
                                Holiday.builder().holidayDate(LocalDate.of(2025, 1, 31))
                                                .description("Tết Nguyên Đán — Mùng 3").build(),
                                Holiday.builder().holidayDate(LocalDate.of(2026, 2, 1))
                                                .description("Tết Nguyên Đán — Mùng 4").build(),
                                Holiday.builder().holidayDate(LocalDate.of(2026, 2, 2))
                                                .description("Tết Nguyên Đán — Mùng 5").build(),
                                Holiday.builder().holidayDate(LocalDate.of(2026, 4, 18))
                                                .description("Giỗ Tổ Hùng Vương (10/3 Âm lịch)").build(),
                                Holiday.builder().holidayDate(LocalDate.of(2026, 4, 30))
                                                .description("Ngày Giải phóng Miền Nam").build(),
                                Holiday.builder().holidayDate(LocalDate.of(2026, 5, 1))
                                                .description("Ngày Quốc tế Lao động").build(),
                                Holiday.builder().holidayDate(LocalDate.of(2026, 9, 2))
                                                .description("Ngày Quốc khánh").build());
                holidayRepository.saveAll(holidays);
                LeaveType annualLeave = leaveTypeRepository.save(LeaveType.builder()
                                .code("LT-ANNUAL")
                                .name("Annual Leave")
                                .requiresProof(false)
                                .isPaid(LeaveTypeEnum.ANNUAL)
                                .build());

                LeaveType sickLeave = leaveTypeRepository.save(LeaveType.builder()
                                .code("LT-SICK")
                                .name("Sick Leave")
                                .requiresProof(true)
                                .isPaid(LeaveTypeEnum.SICK)
                                .build());

                LeaveType unpaidLeave = leaveTypeRepository.save(LeaveType.builder()
                                .code("LT-UNPAID")
                                .name("Unpaid Leave")
                                .requiresProof(false)
                                .isPaid(LeaveTypeEnum.UNPAID)
                                .build());
                leaveBalanceRepository.save(LeaveBalance.builder()
                                .employee(employee1)
                                .year(2026)
                                .leaveType(sickLeave)
                                .totalDays(new BigDecimal("100"))
                                .usedDays(BigDecimal.ZERO)
                                .pendingDays(BigDecimal.ZERO)
                                .build());
                leaveBalanceRepository.save(LeaveBalance.builder()
                                .employee(employee2)
                                .year(2026)
                                .leaveType(sickLeave)
                                .totalDays(new BigDecimal("100"))
                                .usedDays(BigDecimal.ZERO)
                                .pendingDays(BigDecimal.ZERO)
                                .build());
                // if (departmentRepository.count() > 0) {
                // log.info("[Seeder] Data đã tồn tại — bỏ qua seed.");
                // return;
                // }
                // log.info("[Seeder] Bắt đầu seed dữ liệu...");

                // // ── 1. LEAVE TYPES ──────────────────────────────────────────────────
                // LeaveType annualLeave = leaveTypeRepository.save(LeaveType.builder()
                // .code("LT-ANNUAL")
                // .name("Annual Leave")
                // .requiresProof(false)
                // .isPaid(LeaveTypeEnum.ANNUAL)
                // .build());

                // LeaveType sickLeave = leaveTypeRepository.save(LeaveType.builder()
                // .code("LT-SICK")
                // .name("Sick Leave")
                // .requiresProof(true)
                // .isPaid(LeaveTypeEnum.SICK)
                // .build());

                // LeaveType unpaidLeave = leaveTypeRepository.save(LeaveType.builder()
                // .code("LT-UNPAID")
                // .name("Unpaid Leave")
                // .requiresProof(false)
                // .isPaid(LeaveTypeEnum.UNPAID)
                // .build());

                // log.info("[Seeder] ✔ LeaveTypes: 3 bản ghi");

                // // ── 2. DEPARTMENTS (chưa có manager — tránh circular dependency) ────
                // Department itDept = departmentRepository.save(Department.builder()
                // .code("DEPT-IT")
                // .name(DeparmentNameEnum.IT)
                // .build());

                // Department hrDept = departmentRepository.save(Department.builder()
                // .code("DEPT-HR")
                // .name(DeparmentNameEnum.HR)
                // .build());

                // log.info("[Seeder] ✔ Departments: 2 bản ghi (chưa có manager)");

                // // ── 3. EMPLOYEES ─────────────────────────────────────────────────────
                // // ── IT Department ────────────────────────────────────────────────────
                // // IT Head (không có manager cấp trên)
                // Employee itHead = employeeRepository.save(Employee.builder()
                // .fullName("Nguyễn Văn An")
                // .email("an.nguyen@company.com")
                // .positionLevel(5)
                // .department(itDept)
                // .manager(null)
                // .status("ACTIVE")
                // .build());

                // // Senior Dev — báo cáo IT Head
                // Employee seniorDev1 = employeeRepository.save(Employee.builder()
                // .fullName("Trần Thị Bích")
                // .email("bich.tran@company.com")
                // .positionLevel(3)
                // .department(itDept)
                // .manager(itHead)
                // .status("ACTIVE")
                // .build());

                // Employee seniorDev2 = employeeRepository.save(Employee.builder()
                // .fullName("Lê Văn Cường")
                // .email("cuong.le@company.com")
                // .positionLevel(3)
                // .department(itDept)
                // .manager(itHead)
                // .status("ACTIVE")
                // .build());

                // // Junior Dev — báo cáo Senior Dev 1
                // Employee juniorDev1 = employeeRepository.save(Employee.builder()
                // .fullName("Phạm Thị Dung")
                // .email("dung.pham@company.com")
                // .positionLevel(1)
                // .department(itDept)
                // .manager(seniorDev1)
                // .status("ACTIVE")
                // .build());

                // Employee juniorDev2 = employeeRepository.save(Employee.builder()
                // .fullName("Hoàng Văn Em")
                // .email("em.hoang@company.com")
                // .positionLevel(1)
                // .department(itDept)
                // .manager(seniorDev1)
                // .status("ACTIVE")
                // .build());

                // // ── HR Department ─────────────────────────────────────────────────────
                // // HR Head — báo cáo IT Head (giám đốc chung)
                // Employee hrHead = employeeRepository.save(Employee.builder()
                // .fullName("Vũ Thị Phương")
                // .email("phuong.vu@company.com")
                // .positionLevel(5)
                // .department(hrDept)
                // .manager(null)
                // .status("ACTIVE")
                // .build());

                // Employee hrStaff1 = employeeRepository.save(Employee.builder()
                // .fullName("Đinh Văn Quân")
                // .email("quan.dinh@company.com")
                // .positionLevel(2)
                // .department(hrDept)
                // .manager(hrHead)
                // .status("ACTIVE")
                // .build());

                // Employee hrStaff3 = employeeRepository.save(Employee.builder()
                // .fullName("demo")
                // .email("demo@example.org")
                // .positionLevel(2)
                // .department(hrDept)
                // .manager(hrHead)
                // .status("ACTIVE")
                // .build());

                // Employee hrStaff2 = employeeRepository.save(Employee.builder()
                // .fullName("Bùi Thị Hoa")
                // .email("hoa.bui@company.com")
                // .positionLevel(2)
                // .department(hrDept)
                // .manager(hrHead)
                // .status("ACTIVE")
                // .build());

                // log.info("[Seeder] ✔ Employees: 8 bản ghi");

                // // ── 4. GẮN MANAGER CHO DEPARTMENTS (giải quyết circular dependency) ──
                // itDept.setManager(itHead);
                // departmentRepository.save(itDept);

                // hrDept.setManager(hrHead);
                // departmentRepository.save(hrDept);

                // log.info("[Seeder] ✔ Departments: manager đã được gán");

                // // ── 5. HOLIDAYS (ngày lễ Việt Nam 2025) ─────────────────────────────
                // List<Holiday> holidays = List.of(
                // Holiday.builder().holidayDate(LocalDate.of(2025, 1, 1))
                // .description("Tết Dương lịch").build(),
                // Holiday.builder().holidayDate(LocalDate.of(2025, 1, 28))
                // .description("Tết Nguyên Đán — Giao thừa").build(),
                // Holiday.builder().holidayDate(LocalDate.of(2025, 1, 29))
                // .description("Tết Nguyên Đán — Mùng 1").build(),
                // Holiday.builder().holidayDate(LocalDate.of(2025, 1, 30))
                // .description("Tết Nguyên Đán — Mùng 2").build(),
                // Holiday.builder().holidayDate(LocalDate.of(2025, 1, 31))
                // .description("Tết Nguyên Đán — Mùng 3").build(),
                // Holiday.builder().holidayDate(LocalDate.of(2025, 2, 1))
                // .description("Tết Nguyên Đán — Mùng 4").build(),
                // Holiday.builder().holidayDate(LocalDate.of(2025, 2, 2))
                // .description("Tết Nguyên Đán — Mùng 5").build(),
                // Holiday.builder().holidayDate(LocalDate.of(2025, 4, 18))
                // .description("Giỗ Tổ Hùng Vương (10/3 Âm lịch)").build(),
                // Holiday.builder().holidayDate(LocalDate.of(2025, 4, 30))
                // .description("Ngày Giải phóng Miền Nam").build(),
                // Holiday.builder().holidayDate(LocalDate.of(2025, 5, 1))
                // .description("Ngày Quốc tế Lao động").build(),
                // Holiday.builder().holidayDate(LocalDate.of(2025, 9, 2))
                // .description("Ngày Quốc khánh").build());
                // holidayRepository.saveAll(holidays);
                // log.info("[Seeder] ✔ Holidays: {} bản ghi", holidays.size());

                // // ── 6. LEAVE BALANCES (năm 2025, mỗi nhân viên × 3 loại nghỉ) ───────
                // List<Employee> allEmployees = List.of(
                // itHead, seniorDev1, seniorDev2, juniorDev1, juniorDev2,
                // hrHead, hrStaff1, hrStaff2, hrStaff3);

                // for (Employee emp : allEmployees) {
                // leaveBalanceRepository.save(LeaveBalance.builder()
                // .employee(emp)
                // .leaveType(annualLeave)
                // .year(2025)
                // .totalDays(new BigDecimal("12.0"))
                // .usedDays(BigDecimal.ZERO)
                // .pendingDays(BigDecimal.ZERO)
                // .build());

                // leaveBalanceRepository.save(LeaveBalance.builder()
                // .employee(emp)
                // .leaveType(sickLeave)
                // .year(2025)
                // .totalDays(new BigDecimal("10.0"))
                // .usedDays(BigDecimal.ZERO)
                // .pendingDays(BigDecimal.ZERO)
                // .build());

                // leaveBalanceRepository.save(LeaveBalance.builder()
                // .employee(emp)
                // .leaveType(unpaidLeave)
                // .year(2025)
                // .totalDays(new BigDecimal("30.0"))
                // .usedDays(BigDecimal.ZERO)
                // .pendingDays(BigDecimal.ZERO)
                // .build());
                // }
                // log.info("[Seeder] ✔ LeaveBalances: {} bản ghi", allEmployees.size() * 3);

                // // ── 7. LEAVE REQUESTS ─────────────────────────────────────────────────

                // // Request 1: APPROVED — juniorDev1 xin nghỉ phép năm (đã duyệt)
                // LeaveRequest req1 = leaveRequestRepository.save(LeaveRequest.builder()
                // .fullName(juniorDev1.getFullName())
                // .businessKey("LR-2025-0001")
                // .employee(juniorDev1)
                // .currentAssignee(seniorDev1)
                // .currentAssigneeName(seniorDev1.getFullName())
                // .leaveType(annualLeave)
                // .startDate(LocalDateTime.of(2025, 3, 10, 8, 0))
                // .endDate(LocalDateTime.of(2025, 3, 12, 17, 0))
                // .leaveSession(LeaveSessionEnum.ALL_DAY)
                // .totalWorkingDays(new BigDecimal("3.0"))
                // .reason("Đi du lịch nghỉ hè cùng gia đình.")
                // .status(LeaveRequestStatusEnum.APPROVED)
                // .attachmentUrl(null)
                // .build());

                // // Cập nhật leave balance tương ứng
                // leaveBalanceRepository.findByEmployeeAndLeaveTypeAndYear(juniorDev1,
                // annualLeave, 2025)
                // .ifPresent(lb -> {
                // lb.setUsedDays(new BigDecimal("3.0"));
                // leaveBalanceRepository.save(lb);
                // });

                // // Request 2: APPROVED — seniorDev2 xin nghỉ ốm buổi sáng
                // LeaveRequest req2 = leaveRequestRepository.save(LeaveRequest.builder()
                // .fullName(seniorDev2.getFullName())
                // .businessKey("LR-2025-0002")
                // .employee(seniorDev2)
                // .currentAssignee(itHead)
                // .currentAssigneeName(itHead.getFullName())
                // .leaveType(sickLeave)
                // .startDate(LocalDateTime.of(2025, 4, 7, 8, 0))
                // .endDate(LocalDateTime.of(2025, 4, 7, 12, 0))
                // .leaveSession(LeaveSessionEnum.MORNING)
                // .totalWorkingDays(new BigDecimal("0.5"))
                // .reason("Bị sốt, cần nghỉ ngơi buổi sáng.")
                // .status(LeaveRequestStatusEnum.APPROVED)
                // .attachmentUrl("https://storage.company.com/medical/2025/cert-le-cuong-040725.pdf")
                // .build());

                // leaveBalanceRepository.findByEmployeeAndLeaveTypeAndYear(seniorDev2,
                // sickLeave, 2025)
                // .ifPresent(lb -> {
                // lb.setUsedDays(new BigDecimal("0.5"));
                // leaveBalanceRepository.save(lb);
                // });

                // // Request 3: REJECTED — hrStaff1 xin nghỉ không lương
                // LeaveRequest req3 = leaveRequestRepository.save(LeaveRequest.builder()
                // .fullName(hrStaff1.getFullName())
                // .businessKey("LR-2025-0003")
                // .employee(hrStaff1)
                // .currentAssignee(hrHead)
                // .currentAssigneeName(hrHead.getFullName())
                // .leaveType(unpaidLeave)
                // .startDate(LocalDateTime.of(2025, 5, 5, 8, 0))
                // .endDate(LocalDateTime.of(2025, 5, 9, 17, 0))
                // .leaveSession(LeaveSessionEnum.ALL_DAY)
                // .totalWorkingDays(new BigDecimal("5.0"))
                // .reason("Cần xử lý việc cá nhân quan trọng.")
                // .status(LeaveRequestStatusEnum.REJECTED)
                // .attachmentUrl(null)
                // .build());

                // // Request 4: PENDING — juniorDev2 xin nghỉ phép năm buổi chiều
                // LeaveRequest req4 = leaveRequestRepository.save(LeaveRequest.builder()
                // .fullName(juniorDev2.getFullName())
                // .businessKey("LR-2025-0004")
                // .employee(juniorDev2)
                // .currentAssignee(seniorDev1)
                // .currentAssigneeName(seniorDev1.getFullName())
                // .leaveType(annualLeave)
                // .startDate(LocalDateTime.of(2025, 6, 20, 13, 0))
                // .endDate(LocalDateTime.of(2025, 6, 20, 17, 0))
                // .leaveSession(LeaveSessionEnum.AFTERNOON)
                // .totalWorkingDays(new BigDecimal("0.5"))
                // .reason("Có lịch hẹn với bác sĩ vào buổi chiều.")
                // .status(LeaveRequestStatusEnum.PENDING)
                // .attachmentUrl(null)
                // .build());

                // leaveBalanceRepository.findByEmployeeAndLeaveTypeAndYear(juniorDev2,
                // annualLeave, 2025)
                // .ifPresent(lb -> {
                // lb.setPendingDays(new BigDecimal("0.5"));
                // leaveBalanceRepository.save(lb);
                // });

                // // Request 5: PENDING — hrStaff2 xin nghỉ ốm
                // LeaveRequest req5 = leaveRequestRepository.save(LeaveRequest.builder()
                // .fullName(hrStaff2.getFullName())
                // .businessKey("LR-2025-0005")
                // .employee(hrStaff2)
                // .currentAssignee(hrHead)
                // .currentAssigneeName(hrHead.getFullName())
                // .leaveType(sickLeave)
                // .startDate(LocalDateTime.of(2025, 6, 23, 8, 0))
                // .endDate(LocalDateTime.of(2025, 6, 24, 17, 0))
                // .leaveSession(LeaveSessionEnum.ALL_DAY)
                // .totalWorkingDays(new BigDecimal("2.0"))
                // .reason("Bị viêm họng, có giấy chứng nhận bác sĩ.")
                // .status(LeaveRequestStatusEnum.PENDING)
                // .attachmentUrl("https://storage.company.com/medical/2025/cert-bui-hoa-062325.pdf")
                // .build());

                // leaveBalanceRepository.findByEmployeeAndLeaveTypeAndYear(hrStaff2, sickLeave,
                // 2025)
                // .ifPresent(lb -> {
                // lb.setPendingDays(new BigDecimal("2.0"));
                // leaveBalanceRepository.save(lb);
                // });

                // log.info("[Seeder] ✔ LeaveRequests: 5 bản ghi");

                // // ── 8. APPROVAL HISTORIES ─────────────────────────────────────────────
                // // Lịch sử duyệt cho Request 1 (APPROVED) — 2 cấp duyệt
                // approvalHistoryRepository.save(ApprovalHistory.builder()
                // .leaveRequestId(req1.getId())
                // .approverEmail(seniorDev1.getEmail())
                // .level(1)
                // .action("APPROVED")
                // .comment("Đồng ý. Đã sắp xếp người thay thế trong thời gian này.")
                // .build());

                // approvalHistoryRepository.save(ApprovalHistory.builder()
                // .leaveRequestId(req1.getId())
                // .approverEmail(itHead.getEmail())
                // .level(2)
                // .action("APPROVED")
                // .comment("Đã xác nhận. Chúc em đi nghỉ vui vẻ.")
                // .build());

                // // Lịch sử duyệt cho Request 2 (APPROVED) — 1 cấp duyệt (sick leave buổisáng)
                // approvalHistoryRepository.save(ApprovalHistory.builder()
                // .leaveRequestId(req2.getId())
                // .approverEmail(itHead.getEmail())
                // .level(1)
                // .action("APPROVED")
                // .comment("Đồng ý. Mau khỏi bệnh nhé.")
                // .build());

                // // Lịch sử duyệt cho Request 3 (REJECTED) — bị từ chối
                // approvalHistoryRepository.save(ApprovalHistory.builder()
                // .leaveRequestId(req3.getId())
                // .approverEmail(hrHead.getEmail())
                // .level(1)
                // .action("REJECTED")
                // .comment("Tháng 5 là cao điểm công việc, không thể bố trí nghỉ 5 ngày
                // liêntiếp. Vui lòng đăng ký lại vào tháng khác.")
                // .build());

                // log.info("[Seeder] ✔ ApprovalHistories: 3 bản ghi");
                // log.info("[Seeder] ✅ Seed dữ liệu hoàn tất!");
        }
}
