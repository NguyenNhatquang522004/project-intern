package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EntityScan(basePackages = {
		"com.example.demo", 
		"org.springframework.modulith.events.jpa" 
})
public class DemoApplication {

	public static void main(String[] args) {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		SpringApplication.run(DemoApplication.class, args);
	}

}
