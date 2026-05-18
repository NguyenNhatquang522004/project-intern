package com.example.demo.common.Mdc;

import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class CamundaMdcAspect {

    @Around("@annotation(io.camunda.zeebe.spring.client.annotation.JobWorker)")
    public Object profile(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. Lấy thông tin từ ActivatedJob (tham số của hàm)
        Object[] args = joinPoint.getArgs();
        ActivatedJob job = null;
        for (Object arg : args) {
            if (arg instanceof ActivatedJob) {
                job = (ActivatedJob) arg;
                break;
            }
        }

        if (job != null) {
            MDC.put("processInstanceKey", String.valueOf(job.getProcessInstanceKey()));
            MDC.put("bpmnProcessId", job.getBpmnProcessId());
            MDC.put("elementId", job.getElementId());
            MDC.put("jobKey", String.valueOf(job.getKey()));

            Map<String, Object> variables = job.getVariablesAsMap();
            if (variables.containsKey("employeeId")) {
                MDC.put("employeeId", String.valueOf(variables.get("employeeId")));
            }
        }
        try {
            return joinPoint.proceed();
        } finally {
            MDC.clear();
        }
    }
}
