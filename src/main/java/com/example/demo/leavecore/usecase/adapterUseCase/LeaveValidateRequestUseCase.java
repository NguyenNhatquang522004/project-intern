package com.example.demo.leavecore.usecase.adapterUseCase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.common.Enum.LeaveSessionEnum;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveValidRespones;
import com.example.demo.leavecore.delivery.Mapper.LeaveRequestMapper;
import com.example.demo.leavecore.domain.IRepository.IRepositoryEmployee;
import com.example.demo.leavecore.domain.IRepository.IRepositoryHoliday;
import com.example.demo.leavecore.domain.IRepository.IRepositoryLeaveBalance;
import com.example.demo.leavecore.domain.IRepository.IRepositoryLeaveRequest;
import com.example.demo.leavecore.domain.IRepository.IRepositoryLeaveType;
import com.example.demo.leavecore.domain.entity.Employee;
import com.example.demo.leavecore.domain.entity.Holiday;
import com.example.demo.leavecore.domain.entity.LeaveBalance;
import com.example.demo.leavecore.domain.entity.LeaveRequest;
import com.example.demo.leavecore.domain.entity.LeaveType;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveValidateRequestUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveValidateRequestUseCase implements ILeaveValidateRequestUseCase {

    private final IRepositoryHoliday repositoryHoliday;
    private final IRepositoryLeaveRequest repositoryLeaveRequest;
    private final IRepositoryLeaveBalance repositoryLeaveBalance;
    private final IRepositoryEmployee repositoryEmployee;
    private final LeaveRequestMapper leaveRequestMapper;
    private final IRepositoryLeaveType leaveTypeRepository;

    @Override
    public BaseResponse<LeaveValidRespones> validate(LeaveRequestCreateRequest request) {
        try {
            if (request.startDate().isAfter(request.endDate())) {
                return BaseResponse.<LeaveValidRespones>builder().code("400")
                        .message("Start date is after end date").data(LeaveValidRespones.builder().isvalid(false)
                                .build())
                        .build();
            }
            Optional<Employee> employee = repositoryEmployee.findById(request.employeeId());
            if (employee.isEmpty()) {
                return BaseResponse.<LeaveValidRespones>builder().code("400").message("Employee not found")
                        .data(LeaveValidRespones.builder().isvalid(false).build())
                        .build();
            }
            Optional<LeaveType> leaveType = leaveTypeRepository.findByType(request.leaveType());
            if (leaveType.isEmpty()) {
                return BaseResponse.<LeaveValidRespones>builder().code("400").message("Leave type not found")
                        .data(LeaveValidRespones.builder().isvalid(false).build())
                        .build();
            }
            List<LeaveBalance> leaveBalances = repositoryLeaveBalance.findByEmployee_IdAndYear(request.employeeId(),
                    2025);
            log.info("Leave balances: {}", leaveBalances);
            if (leaveBalances.isEmpty() || leaveBalances.size() > 1) {
                return BaseResponse.<LeaveValidRespones>builder().code("400").message("Leave balance not found")
                        .data(LeaveValidRespones.builder().isvalid(false).build())
                        .build();
            }
            LeaveBalance leaveBalance = leaveBalances.get(0);
            log.info("Leave balance: {}", leaveBalance);
            BigDecimal requestTotalDays = calculateWorkingDays(request.startDate(), request.endDate(),
                    request.leaveSession());
            log.info("Request total days: {}", requestTotalDays);
            if (requestTotalDays.compareTo(BigDecimal.ZERO) <= 1) {
                if (requestTotalDays.compareTo(BigDecimal.ZERO) <= 0) {
                    return BaseResponse.<LeaveValidRespones>builder().code("400")
                            .message("Leave request total days is zero or negative")
                            .data(LeaveValidRespones.builder().isvalid(false).build())
                            .build();
                }
            }

            BigDecimal remainingdays = leaveBalance.getTotalDays()
                    .subtract(leaveBalance.getUsedDays().add(leaveBalance.getPendingDays()));
            if (remainingdays.compareTo(requestTotalDays) <= 0) {
                return BaseResponse.<LeaveValidRespones>builder().code("400").message("Leave balance is not enough")
                        .data(LeaveValidRespones.builder().isvalid(false).build())
                        .build();
            }
            String bussinesskey = generateBusinessKey();
            LeaveRequest entity = leaveRequestMapper.toEntity(request);
            entity.setEmployee(employee.get());
            entity.setLeaveType(leaveType.get());
            entity.setBusinessKey(bussinesskey);
            entity.setStatus(LeaveRequestStatusEnum.PENDING);
            entity.setTotalWorkingDays(requestTotalDays);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setReason(request.reason());
            repositoryLeaveRequest.save(entity);
            leaveBalance.setPendingDays(leaveBalance.getPendingDays().add(requestTotalDays));
            repositoryLeaveBalance.save(leaveBalance);
            return BaseResponse.<LeaveValidRespones>builder().code("200")
                    .message("Leave request validated successfully")
                    .data(LeaveValidRespones.builder().isvalid(true).totalWorkingDays(requestTotalDays)
                            .businessKey(bussinesskey)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Error validating leave request", e);
            return BaseResponse.<LeaveValidRespones>builder().code("500").message("Internal server error")
                    .data(LeaveValidRespones.builder().isvalid(false).build())
                    .build();
        }
    }

    public BigDecimal calculateWorkingDays(LocalDateTime start, LocalDateTime end, LeaveSessionEnum session) {
        if (start.isAfter(end))
            return BigDecimal.ZERO;

        List<LocalDate> holidays = repositoryHoliday.findByHolidayDateBetween(
                start.toLocalDate(), end.toLocalDate())
                .stream()
                .map(Holiday::getHolidayDate)
                .toList();

        double totalDays = 0;
        LocalDate current = start.toLocalDate();
        LocalDate last = end.toLocalDate();

        while (!current.isAfter(last)) {
            if (!isWeekend(current) && !holidays.contains(current)) {
                totalDays += 1.0;
            }
            current = current.plusDays(1);
        }
        if (session == LeaveSessionEnum.MORNING || session == LeaveSessionEnum.AFTERNOON) {
            totalDays -= 0.5;
        }

        return BigDecimal.valueOf(totalDays).setScale(1, RoundingMode.HALF_UP);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    private String generateBusinessKey() {
        return String.format("LR-%s-%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
