package io.github.internlm.lagent4j.agents;

import io.github.internlm.lagent4j.actions.ActionExecutor;
import io.github.internlm.lagent4j.agents.aggregator.DefaultAggregator;
import io.github.internlm.lagent4j.llms.BaseLLM;
import io.github.internlm.lagent4j.memory.MemoryManager;
import io.github.internlm.lagent4j.prompts.parsers.StrParser;
import io.github.internlm.lagent4j.schema.AgentMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Agent类的单元测试
 */
public class AgentTest {

    @Mock
    private BaseLLM mockLlm;
    
    @Mock
    private StrParser mockParser;
    
    @Mock
    private DefaultAggregator mockAggregator;
    
    @Mock
    private ActionExecutor mockExecutor;
    
    private Agent agent;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置模拟对象行为
        when(mockLlm.chat(any())).thenReturn("Mock response");
        when(mockLlm.chat(any(), any())).thenReturn("Mock response");
        when(mockLlm.chatAsync(any())).thenReturn(CompletableFuture.completedFuture("Mock response"));
        when(mockLlm.chatAsync(any(), any())).thenReturn(CompletableFuture.completedFuture("Mock response"));

        when(mockAggregator.aggregate(any(), any(), any(), any())).thenReturn(List.of(Map.of("role", "system", "content", "You are a helpful assistant.")));

        when(mockParser.parseResponse(any())).thenReturn("Mock response");
        
        // 设置工具执行器的行为
        when(mockExecutor.getActionDescriptions()).thenReturn(new ArrayList<>());
        when(mockExecutor.process(any())).thenReturn(new AgentMessage("action_executor", "Mock action result"));

        // 创建Agent实例
        agent = new SyncAgent(mockLlm, "You are a helpful assistant.", null, mockParser, mockAggregator, mockExecutor, "test_agent", "Test Agent");
    }
    
    @Test
    public void testProcessMessage() {
        // 创建用户消息
        AgentMessage userMessage = new AgentMessage("user", "Hello");
        
        // 处理消息
        AgentMessage response = agent.process(userMessage,0);
        
        // 验证响应
        assertNotNull(response);
        assertEquals("test_agent", response.getSender());
        assertEquals("Mock response", response.getContent());
        
        // 验证LLM被调用
        verify(mockLlm).chat(any());
    }
    
    @Test
    public void testProcessString() {
        // 处理字符串消息
        AgentMessage response = agent.process(AgentMessage.builder().content("Hello").build(),0);
        
        // 验证响应
        assertNotNull(response);
        assertEquals("test_agent", response.getSender());
        assertEquals("Mock response", response.getContent());
        
        // 验证LLM被调用
        verify(mockLlm).chat(any());
    }
    
    @Test
    public void testUpdateMemory() {
        // 创建用户消息
        AgentMessage userMessage = new AgentMessage("user", "Hello");
        
        // 更新内存
        agent.updateMemory(userMessage, 0);
        
        // 验证内存被更新
        assertFalse(agent.getMemory().get(0).isEmpty());
        assertEquals(1, agent.getMemory().get(0).size());
        assertEquals("Hello", agent.getMemory().get(0).getLast().getContent());
    }
    
    @Test
    public void testReset() {
        // 创建用户消息
        AgentMessage userMessage = new AgentMessage("user", "Hello");
        
        // 更新内存
        agent.updateMemory(userMessage, 0);
        
        // 验证内存不为空
        assertFalse(agent.getMemory().get(0).isEmpty());
        
        // 重置内存
        agent.getMemory().clearAll();
        
        // 验证内存为空
        assertTrue(agent.getMemory().get(0).isEmpty());
    }
    
    @Test
    public void testProcessWithToolExecution() {
        // 准备测试数据
        AgentMessage message = new AgentMessage("user", "Test message");
        Map<String, Object> toolResponse = new HashMap<>();
        toolResponse.put("action", "test_tool");
        toolResponse.put("parameters", Map.of("param1", "value1"));
        
        // 设置解析器返回工具调用
        when(mockParser.parseResponse(any())).thenReturn(toolResponse);
        
        // 执行测试
        AgentMessage response = agent.process(message,0);
        
        // 验证工具执行器被调用
        verify(mockExecutor, times(1)).process(any());
        
        // 验证响应
        assertNotNull(response);
        assertEquals("action_executor", response.getSender());
        assertEquals("Mock action result", response.getContent());
    }
    

    
    @Test
    public void testConstructorWithMinimalParameters() {
        // 使用最小参数创建Agent
        Agent minimalAgent = new SyncAgent(mockLlm, "You are a helpful assistant.");
        
        // 验证默认值
        assertNotNull(minimalAgent.getMemory());
        assertNotNull(minimalAgent.getAggregator());
        assertNull(minimalAgent.getActionExecutor());
        assertEquals("Agent", minimalAgent.getName());
        assertNull(minimalAgent.getDescription());
    }
    
    @Test
    public void testConstructorWithMemoryConfig() {
        // 创建内存配置
        Map<String, Object> memoryConfig = new HashMap<>();
        memoryConfig.put("capacity", 10);
        
        // 使用内存配置创建Agent
        Agent agentWithMemoryConfig = new SyncAgent(
            mockLlm, 
            "You are a helpful assistant.", 
            memoryConfig, 
            null, 
            null, 
            null,
            null, 
            null
        );
        
        // 验证内存管理器被创建
        assertNotNull(agentWithMemoryConfig.getMemory());
        assertTrue(agentWithMemoryConfig.getMemory() instanceof MemoryManager);
    }
    
    @Test
    public void testUpdateTemplateWithToolInfo() {
        // 准备测试数据
        List<Map<String, Object>> toolDescriptions = new ArrayList<>();
        Map<String, Object> toolDesc = new HashMap<>();
        toolDesc.put("name", "test_tool");
        toolDesc.put("description", "Test tool description");
        toolDesc.put("parameters", Map.of("param1", "string"));
        toolDescriptions.add(toolDesc);
        
        // 设置工具执行器返回工具描述
        when(mockExecutor.getActionDescriptions()).thenReturn(toolDescriptions);
        
        // 创建新的Agent实例
        Agent agentWithTools = new SyncAgent(
            mockLlm, 
            "Base prompt", 
            null, 
            mockParser, 
            mockAggregator, 
            mockExecutor,
            "test_agent", 
            "Test Agent"
        );
        
        // 验证模板被更新
        String template = agentWithTools.getTemplate().toString();
        assertTrue(template.contains("可用工具列表"));
        assertTrue(template.contains("test_tool"));
        assertTrue(template.contains("Test tool description"));
    }
} 