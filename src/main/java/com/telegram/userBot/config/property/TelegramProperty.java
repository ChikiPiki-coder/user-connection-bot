package com.telegram.userBot.config.property;

import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "applications.telegram")
public class TelegramProperty {
    @NotNull
    private String token;
    @NotNull
    private String botName;
}
