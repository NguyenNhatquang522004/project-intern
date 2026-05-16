package com.example.demo.leavecore.infrastructure.postgres.Repository;

import com.example.demo.leavecore.domain.entity.Holiday;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByHolidayDate(LocalDate date);
}
