package com.techcraft.techcraftbackend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeminiConfigTest {

    @Test
    void normalizeBaseUrl_StripsVersionSuffixAndTrailingSlash() {
        assertEquals("https://generativelanguage.googleapis.com",
                GeminiConfig.normalizeBaseUrl("https://generativelanguage.googleapis.com/v1beta"));
        assertEquals("https://generativelanguage.googleapis.com",
                GeminiConfig.normalizeBaseUrl("https://generativelanguage.googleapis.com/v1beta/"));
        assertEquals("https://generativelanguage.googleapis.com",
                GeminiConfig.normalizeBaseUrl("https://generativelanguage.googleapis.com/v1"));
        assertEquals("https://generativelanguage.googleapis.com",
                GeminiConfig.normalizeBaseUrl("https://generativelanguage.googleapis.com/"));
    }

    @Test
    void resolveApiVersion_UsesConfiguredOrExtractsFromBaseUrl() {
        assertEquals("v1beta",
                GeminiConfig.resolveApiVersion("https://generativelanguage.googleapis.com", "v1beta"));
        assertEquals("v1",
                GeminiConfig.resolveApiVersion("https://generativelanguage.googleapis.com/v1", ""));
        assertEquals("v1beta",
                GeminiConfig.resolveApiVersion("https://generativelanguage.googleapis.com", null));
    }
}
