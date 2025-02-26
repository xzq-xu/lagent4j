package io.github.internlm.lagent4j.prompts.parsers;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolParser类的单元测试
 */
public class ToolParserTest {

    @Test
    public void testParseResponseWithToolCall() {
        // 创建工具解析器
        ToolParser parser = new ToolParser("code_interpreter", "```python", "```");
        
        // 模拟LLM响应，包含代码块
        String response = "我将使用Python来解决这个问题。\n\n```python\ndef fibonacci(n):\n    if n <= 1:\n        return n\n    return fibonacci(n-1) + fibonacci(n-2)\n\nprint(fibonacci(10))\n```\n\n这个函数使用递归方法计算斐波那契数列。";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容
        assertEquals("code_interpreter", resultMap.get("tool_type"));
        assertEquals(1, resultMap.get("status"));
        assertEquals("我将使用Python来解决这个问题。", resultMap.get("thought"));
        assertEquals("def fibonacci(n):\n    if n <= 1:\n        return n\n    return fibonacci(n-1) + fibonacci(n-2)\n\nprint(fibonacci(10))", resultMap.get("action"));
    }
    
    @Test
    public void testParseResponseWithoutToolCall() {
        // 创建工具解析器
        ToolParser parser = new ToolParser("code_interpreter");
        
        // 模拟LLM响应，不包含代码块
        String response = "斐波那契数列是一个以递归方式定义的数列，其中每个数字是前两个数字的和。数列的前几个数字是：0, 1, 1, 2, 3, 5, 8, 13, 21, ...";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容
        assertEquals("code_interpreter", resultMap.get("tool_type"));
        assertEquals(0, resultMap.get("status"));
        assertEquals(response, resultMap.get("thought"));
        assertEquals("", resultMap.get("action"));
    }
    
    @Test
    public void testParseResponseWithMultipleCodeBlocks() {
        // 创建工具解析器
        ToolParser parser = new ToolParser("code_interpreter", "```python", "```");
        
        // 模拟LLM响应，包含多个代码块
        String response = "我将展示两种计算斐波那契数列的方法。\n\n递归方法：\n```python\ndef fibonacci_recursive(n):\n    if n <= 1:\n        return n\n    return fibonacci_recursive(n-1) + fibonacci_recursive(n-2)\n```\n\n迭代方法：\n```python\ndef fibonacci_iterative(n):\n    a, b = 0, 1\n    for _ in range(n):\n        a, b = b, a + b\n    return a\n```";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容 - 应该只提取第一个代码块
        assertEquals("code_interpreter", resultMap.get("tool_type"));
        assertEquals(1, resultMap.get("status"));
        assertEquals("我将展示两种计算斐波那契数列的方法。\n\n递归方法：", resultMap.get("thought"));
        assertEquals("def fibonacci_recursive(n):\n    if n <= 1:\n        return n\n    return fibonacci_recursive(n-1) + fibonacci_recursive(n-2)", resultMap.get("action"));
    }
    
    @Test
    public void testParseResponseWithCustomDelimiters() {
        // 创建使用自定义分隔符的工具解析器
        ToolParser parser = new ToolParser("sql_executor", "```sql", "```");
        
        // 模拟LLM响应，包含SQL代码块
        String response = "我将使用SQL查询来获取数据。\n\n```sql\nSELECT name, age\nFROM users\nWHERE age > 18\nORDER BY age DESC;\n```\n\n这个查询将返回所有年龄大于18的用户，按年龄降序排列。";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容
        assertEquals("sql_executor", resultMap.get("tool_type"));
        assertEquals(1, resultMap.get("status"));
        assertEquals("我将使用SQL查询来获取数据。\n\n", resultMap.get("thought"));
        assertEquals("SELECT name, age\nFROM users\nWHERE age > 18\nORDER BY age DESC;", resultMap.get("action"));
    }
    
    @Test
    public void testParseResponseWithEmptyResponse() {
        // 创建工具解析器
        ToolParser parser = new ToolParser("code_interpreter");
        
        // 空响应
        String response = "";
        
        // 解析响应
        Object result = parser.parseResponse(response);
        
        // 验证结果类型
        assertTrue(result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 验证结果内容
        assertEquals("code_interpreter", resultMap.get("tool_type"));
        assertEquals(0, resultMap.get("status"));
        assertEquals("", resultMap.get("thought"));
        assertEquals("", resultMap.get("action"));
    }
} 