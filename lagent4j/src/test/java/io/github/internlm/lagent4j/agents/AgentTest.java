package io.github.internlm.lagent4j.agents;

import io.github.internlm.lagent4j.agents.aggregator.DefaultAggregator;
import io.github.internlm.lagent4j.llms.BaseLLM;
import io.github.internlm.lagent4j.memory.MemoryManager;
import io.github.internlm.lagent4j.prompts.StrParser;
import io.github.internlm.lagent4j.schema.AgentMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

        // 创建Agent实例
        agent = new Agent(mockLlm, "You are a helpful assistant.", null, mockParser, mockAggregator, "test_agent", "Test Agent");
    }
    
    @Test
    public void testProcessMessage() {
        // 创建用户消息
        AgentMessage userMessage = new AgentMessage("user", "Hello");
        
        // 处理消息
        AgentMessage response = agent.process(userMessage);
        
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
        AgentMessage response = agent.process("Hello");
        
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
        agent.reset(0);
        
        // 验证内存为空
        assertTrue(agent.getMemory().get(0).isEmpty());
    }
    
    @Test
    public void testStateDict() {
        // 获取状态字典
        Map<String, Object> stateDict = agent.stateDict();
        
        // 验证状态字典包含预期的键
        assertTrue(stateDict.containsKey("name"));
        assertTrue(stateDict.containsKey("description"));
        assertTrue(stateDict.containsKey("template"));
        
        // 验证值
        assertEquals("test_agent", stateDict.get("name"));
        assertEquals("Test Agent", stateDict.get("description"));
        assertEquals("You are a helpful assistant.", stateDict.get("template"));
    }
    
    @Test
    public void testConstructorWithMinimalParameters() {
        // 使用最小参数创建Agent
        Agent minimalAgent = new Agent(mockLlm, "You are a helpful assistant.");
        
        // 验证默认值
        assertNotNull(minimalAgent.getMemory());
        assertNotNull(minimalAgent.getAggregator());
        assertEquals("Agent", minimalAgent.getName());
        assertEquals(null, minimalAgent.getDescription());
    }
    
    @Test
    public void testConstructorWithMemoryConfig() {
        // 创建内存配置
        Map<String, Object> memoryConfig = new HashMap<>();
        memoryConfig.put("capacity", 10);
        
        // 使用内存配置创建Agent
        Agent agentWithMemoryConfig = new Agent(mockLlm, "You are a helpful assistant.", memoryConfig, null, null, null, null);
        
        // 验证内存管理器被创建
        assertNotNull(agentWithMemoryConfig.getMemory());
        assertTrue(agentWithMemoryConfig.getMemory() instanceof MemoryManager);
    }
} 