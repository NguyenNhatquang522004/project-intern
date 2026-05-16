package com.example.demo.leavecore.domain.IRepository;

import com.example.demo.leavecore.domain.entity.Holiday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IRepositoryHoliday {
    Holiday save(Holiday holiday);

    Optional<Holiday> findById(Long id);

    void deleteById(Long id);

    List<Holiday> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);
}
