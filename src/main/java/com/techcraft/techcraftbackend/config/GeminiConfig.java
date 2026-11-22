package com.techcraft.techcraftbackend.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
public class GeminiConfig {

    private static final Pattern VERSION_SUFFIX_PATTERN = Pattern.compile("^(.*?)/(v\\d+(?:beta|alpha)?)$");

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${gemini.api-version:v1beta}")
    private String apiVersion;

    @Bean(destroyMethod = "close")
    public Client geminiClient() {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        String effectiveApiVersion = resolveApiVersion(baseUrl, apiVersion);

        HttpOptions.Builder httpOptionsBuilder = HttpOptions.builder()
                .apiVersion(effectiveApiVersion);

        if (!normalizedBaseUrl.isBlank()) {
            httpOptionsBuilder.baseUrl(normalizedBaseUrl);
        }

        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(httpOptionsBuilder.build())
                .build();
    }

    static String normalizeBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            return "";
        }
        String trimmed = rawBaseUrl.trim().replaceAll("/+$", "");
        Matcher matcher = VERSION_SUFFIX_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String rootUrl = matcher.group(1);
            log.warn("gemini.base-url ('{}') chứa hậu tố phiên bản '{}'; tự động chuẩn hóa thành '{}'.",
                    rawBaseUrl, matcher.group(2), rootUrl);
            return rootUrl;
        }
        return trimmed;
    }

    static String resolveApiVersion(String rawBaseUrl, String configuredApiVersion) {
        if (configuredApiVersion != null && !configuredApiVersion.isBlank()) {
            return configuredApiVersion.trim();
        }
        if (rawBaseUrl != null && !rawBaseUrl.isBlank()) {
            String trimmed = rawBaseUrl.trim().replaceAll("/+$", "");
            Matcher matcher = VERSION_SUFFIX_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                return matcher.group(2);
            }
        }
        return "v1beta";
    }
}
