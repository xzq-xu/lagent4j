package com.example.lagent4j.examples;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class WeatherAssistantTest {
    private WeatherAssistant assistant;

    @Before
    public void setUp() {
        String apiKey = "sk-d5d94cd5925f4a2fb33ad536825f124e";
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
    public void testWeatherCheck() throws Exception {
        String result = assistant.checkWeather("Shanghai")
            .get(3000, TimeUnit.SECONDS);
        
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
} 