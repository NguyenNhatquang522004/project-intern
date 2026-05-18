package com.example.demo.leavecore.service;

import org.springframework.stereotype.Service;

import com.example.demo.leavecore.delivery.Dto.ManualTrigger.BeginInput;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.BeginOutput;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BeginService {

    public BeginOutput execute(BeginInput input) {
        log.info("--- [SERVICE] Bắt đầu xử lý Begin ---");
        try {
            log.info("Hello World - sleeping for 8s");
            Thread.sleep(8000);
            log.info("Hello World - wake up");
            return new BeginOutput(true, "Begin task execution success");
        } catch (InterruptedException e) {
            log.error("Begin service sleep interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Begin service interrupted", e);
        }
    }
}
