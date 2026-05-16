package com.example.demo.leavecore.delivery.Handler;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.camunda.zeebe.client.ZeebeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestController
public class MessageCancelHandler {
    private final ZeebeClient zeebeClient;
    @PostMapping("/cancel/{businessKey}")
    public ResponseEntity<String> cancelRequest(@PathVariable String businessKey, 
                                               @RequestParam String reason) {
        log.info("--- [API] Nhận yêu cầu hủy đơn: {} ---", businessKey);

        zeebeClient.newPublishMessageCommand()
                .messageName("Msg_CancelRequest")
                .correlationKey(businessKey)
                .messageId(businessKey + "_CANCEL")
                .timeToLive(Duration.ofMinutes(10))
                .variables(Map.of("cancelReason", reason))
                .send()
                .join(); 

        return ResponseEntity.ok("Tín hiệu hủy đã được gửi lên Cloud cho đơn: " + businessKey);
    }
}
