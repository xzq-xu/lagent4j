package io.github.internlm.lagent4j.prompts.parsers;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonParser类的单元测试
 */
public class JsonParserTest {

    @Test
    public void testParseResponseWithJsonBlock() {
        // 创建JSON解析器
        JsonParser parser = new JsonParser("```json", "```");
        
        // 模拟LLM响应，包含JSON块
        String response = "我将使用web_browser工具来获取网页内容。\n\n```json\n{\n  \"action\": \"web_browser\",\n  \"parameters\": {\n    \"url\": \"https://example.com\"\n  },\n  \"thought\": \"我需要获取example.com的内容来回答问题\"\n}\n```";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容
        assertEquals(1, resultMap.get("status"));
        assertEquals("web_browser", resultMap.get("action"));
        assertEquals("我将使用web_browser工具来获取网页内容。", resultMap.get("thought"));
        
        // 验证嵌套参数
        assertTrue(resultMap.get("parameters") instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) resultMap.get("parameters");
        assertEquals("https://example.com", parameters.get("url"));
    }
    
    @Test
    public void testParseResponseWithoutJsonBlock() {
        // 创建JSON解析器
        JsonParser parser = new JsonParser();
        
        // 模拟LLM响应，不包含JSON块
        String response = "要获取网页内容，你可以使用web_browser工具，并提供要访问的URL。";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容
        assertEquals(0, resultMap.get("status"));
        assertTrue(resultMap.containsKey("error"));
        assertEquals(response, resultMap.get("thought"));
    }
    
    @Test
    public void testParseResponseWithDirectJson() {
        // 创建JSON解析器
        JsonParser parser = new JsonParser();
        
        // 模拟LLM响应，直接是JSON格式
        String response = "{\"action\":\"web_browser\",\"parameters\":{\"url\":\"https://example.com\"},\"thought\":\"我需要获取example.com的内容\"}";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容
        assertEquals(1, resultMap.get("status"));
        assertEquals("web_browser", resultMap.get("action"));
        assertEquals("我需要获取example.com的内容", resultMap.get("thought"));
        
        // 验证嵌套参数
        assertTrue(resultMap.get("parameters") instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) resultMap.get("parameters");
        assertEquals("https://example.com", parameters.get("url"));
    }
    
    @Test
    public void testParseResponseWithInvalidJson() {
        // 创建JSON解析器
        JsonParser parser = new JsonParser("```json", "```");
        
        // 模拟LLM响应，包含无效的JSON块
        String response = "我将使用工具来获取数据。\n\n```json\n{\n  \"action\": \"web_browser\",\n  \"parameters\": {\n    \"url\": \"https://example.com\"\n  },\n  \"thought\": \"我需要获取数据\"\n```";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容
        assertEquals(0, resultMap.get("status"));
        assertTrue(resultMap.containsKey("error"));
        assertEquals("我将使用工具来获取数据。", resultMap.get("thought"));
    }
    
    @Test
    public void testParseResponseWithEmptyResponse() {
        // 创建JSON解析器
        JsonParser parser = new JsonParser();
        
        // 空响应
        String response = "";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容
        assertEquals(0, resultMap.get("status"));
        assertTrue(resultMap.containsKey("error"));
    }
    
    @Test
    public void testParseResponseWithMultipleJsonBlocks() {
        // 创建JSON解析器
        JsonParser parser = new JsonParser("```json", "```");
        
        // 模拟LLM响应，包含多个JSON块
        String response = "我将展示两种工具调用。\n\n第一种：\n```json\n{\n  \"action\": \"web_browser\",\n  \"parameters\": {\n    \"url\": \"https://example.com\"\n  }\n}\n```\n\n第二种：\n```json\n{\n  \"action\": \"calculator\",\n  \"parameters\": {\n    \"expression\": \"2+2\"\n  }\n}\n```";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容 - 应该只提取第一个JSON块
        assertEquals(1, resultMap.get("status"));
        assertEquals("web_browser", resultMap.get("action"));
        assertEquals("我将展示两种工具调用。\n\n第一种：", resultMap.get("thought"));
        
        // 验证嵌套参数
        assertTrue(resultMap.get("parameters") instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) resultMap.get("parameters");
        assertEquals("https://example.com", parameters.get("url"));
    }
} 