package com.example.demo.leavecore.service;

import org.springframework.stereotype.Service;

import com.example.demo.leavecore.delivery.Dto.ManualTrigger.EndEventInput;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.EndEventOutput;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EndEventService {

    public EndEventOutput execute(EndEventInput input) {
        log.info("--- [SERVICE] Bắt đầu xử lý EndEvent: businessKey={} ---", input.businessKey());
        log.info("Executing end execution flow logic (logging, cleanups)");
        log.info("--- [SERVICE] Kết thúc xử lý EndEvent: Thành công ---");
        return new EndEventOutput(true, "End error event handled successfully");
    }
}
