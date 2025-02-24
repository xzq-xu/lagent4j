package com.example.lagent4j.prompts;

import com.alibaba.fastjson2.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolParserTest {
    private final JsonToolParser parser = new JsonToolParser(
        "test",
        "```json\n",
        "\n```"
    );

    @Test
    public void testParseValidResponse() {
        String response = """
            思考：需要调用HTTP工具获取数据
            ```json
            {
                "url": "https://api.example.com/data",
                "method": "GET"
            }
            ```
            """;
        
        Object result = parser.parseResponse(response);
        assertNotNull(result);
        assertTrue(result instanceof JSONObject);
        
        JSONObject json = (JSONObject) result;
        assertEquals("test", json.getString("tool_type"));
        assertTrue(json.getString("thought").contains("思考"));
        assertTrue(json.getString("action").contains("api.example.com"));
    }

    @Test
    public void testParseInvalidResponse() {
        String response = "Invalid response without JSON";
        Object result = parser.parseResponse(response);
        assertNull(result);
    }

    @Test
    public void testValidateJson() {
        assertTrue(parser.validate("{\"key\": \"value\"}"));
        assertFalse(parser.validate("invalid json"));
        assertFalse(parser.validate(""));
        assertFalse(parser.validate(null));
    }
} 