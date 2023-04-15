package com.telegram.userBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class UserBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserBotApplication.class, args);
	}

}
