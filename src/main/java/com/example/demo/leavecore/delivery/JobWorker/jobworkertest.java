package com.example.demo.leavecore.delivery.JobWorker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;

import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class jobworkertest {
    @JobWorker(type = "begin")
    public void endSuccess(final JobClient client, final ActivatedJob job) {

        try {
            log.info("Hello World");
            Thread.sleep(8000);
            log.info("Hello World");
        } catch (Exception e) {

        }
    }

}
