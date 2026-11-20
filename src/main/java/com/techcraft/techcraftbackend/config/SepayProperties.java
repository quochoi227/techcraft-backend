package com.techcraft.techcraftbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sepay")
public class SepayProperties {

    private String bankName;
    private String bankAccount;
    private String accountName;
    private String webhookApiKey;
    private String qrTemplate = "compact";
}
