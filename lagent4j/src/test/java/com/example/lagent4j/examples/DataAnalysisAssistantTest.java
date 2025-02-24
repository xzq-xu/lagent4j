package com.example.lagent4j.examples;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class DataAnalysisAssistantTest {
    private DataAnalysisAssistant assistant;
    private static final String TEST_API_KEY = "sk-d5d94cd5925f4a2fb33ad536825f124e";

    @Before
    public void setUp() {
        // 创建报告目录
        try {
            Files.createDirectories(Path.of("./reports"));
        } catch (Exception e) {
            fail("Failed to create reports directory");
        }

        assistant = new DataAnalysisAssistant(TEST_API_KEY);
    }

    @After
    public void tearDown() {
        assistant.close();
    }

    @Test
    public void testStockAnalysis() throws Exception {
        String result = assistant.analyzeStock("AAPL")
            .get(60, TimeUnit.SECONDS);
        
        assertNotNull(result);
        assertTrue(result.contains("AAPL"));
        
        // 验证数据库中是否有数据
        assertTrue(Files.exists(Path.of("./reports/AAPL_analysis.html")));
    }

    @Test
    public void testReportGeneration() throws Exception {
        // 先进行分析
        assistant.analyzeStock("GOOGL")
            .get(60, TimeUnit.SECONDS);
        
        // 生成报告
        String report = assistant.generateReport("GOOGL")
            .get(30, TimeUnit.SECONDS);
        
        assertNotNull(report);
        assertTrue(report.contains("GOOGL"));
        assertTrue(Files.exists(Path.of("./reports/GOOGL_report.html")));
    }
} 