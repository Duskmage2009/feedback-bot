package com.autoservice.feedbackbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FeedbackBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeedbackBotApplication.class, args);
	}

}
