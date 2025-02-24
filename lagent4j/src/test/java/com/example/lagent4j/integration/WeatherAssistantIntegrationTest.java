package com.example.lagent4j.integration;

import com.example.lagent4j.examples.WeatherAssistant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class WeatherAssistantIntegrationTest {
    private WeatherAssistant assistant;

    @Before
    public void setUp() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable not set");
        }
        assistant = new WeatherAssistant(apiKey);
    }

    @After
    public void tearDown() {
        assistant.close();
    }

    @Test
    public void testWeatherCheckFlow() throws Exception {
        String result = assistant.checkWeather("Shanghai")
            .get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Shanghai") || result.contains("上海"));
    }

    @Test
    public void testWeatherCheckError() throws Exception {
        String result = assistant.checkWeather("NonexistentCity")
            .get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        assertTrue(result.contains("失败") || result.contains("failed"));
    }

    @Test
    public void testMultipleRequests() throws Exception {
        // 测试连续多次请求
        for (int i = 0; i < 3; i++) {
            String result = assistant.checkWeather("Beijing")
                .get(30, TimeUnit.SECONDS);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }

    @Test
    public void testConcurrentRequests() throws Exception {
        // 测试并发请求
        CompletableFuture<String> future1 = assistant.checkWeather("Shanghai");
        CompletableFuture<String> future2 = assistant.checkWeather("Beijing");
        CompletableFuture<String> future3 = assistant.checkWeather("Guangzhou");

        CompletableFuture.allOf(future1, future2, future3)
            .get(60, TimeUnit.SECONDS);

        assertNotNull(future1.get());
        assertNotNull(future2.get());
        assertNotNull(future3.get());
    }
} 