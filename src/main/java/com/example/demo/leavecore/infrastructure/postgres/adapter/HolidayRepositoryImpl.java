package com.example.demo.leavecore.infrastructure.postgres.adapter;

import com.example.demo.leavecore.domain.IRepository.IRepositoryHoliday;
import com.example.demo.leavecore.domain.entity.Holiday;
import com.example.demo.leavecore.infrastructure.postgres.Repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HolidayRepositoryImpl implements IRepositoryHoliday {

    private final HolidayRepository repository;

    @Override
    public List<Holiday> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate) {
        return repository.findByHolidayDateBetween(startDate, endDate);
    }

    @Override
    public Holiday save(Holiday holiday) {
        return repository.save(holiday);
    }

    @Override
    public Optional<Holiday> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
