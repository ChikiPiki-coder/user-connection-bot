package com.telegram.userBot.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses =
    { com.telegram.userBot.client.LamodaClient.class, com.telegram.userBot.client.ScraperClient.class})
public class CloudConfiguration {

}

