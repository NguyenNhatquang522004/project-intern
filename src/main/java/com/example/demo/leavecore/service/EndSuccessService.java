package com.example.demo.leavecore.service;

import org.springframework.stereotype.Service;

import com.example.demo.leavecore.delivery.Dto.ManualTrigger.EndSuccessInput;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.EndSuccessOutput;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EndSuccessService {

    public EndSuccessOutput execute(EndSuccessInput input) {
        log.info("--- [SERVICE] Bắt đầu xử lý EndEventSuccess: businessKey={} ---", input.businessKey());
        log.info("Executing end success execution flow logic");
        log.info("--- [SERVICE] Kết thúc xử lý EndEventSuccess: Thành công ---");
        return new EndSuccessOutput(true, "End success event handled successfully");
    }
}
