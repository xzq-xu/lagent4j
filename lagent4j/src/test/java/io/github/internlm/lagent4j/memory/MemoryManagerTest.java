package io.github.internlm.lagent4j.memory;

import io.github.internlm.lagent4j.schema.AgentMessage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryManager类的单元测试
 */
public class MemoryManagerTest {

    @Test
    public void testGetAndAddMemory() {
        // 创建内存管理器
        MemoryManager manager = new MemoryManager();
        
        // 获取不存在的会话内存，应该自动创建
        Memory memory = manager.get(1);
        assertNotNull(memory);
        assertTrue(memory.isEmpty());
        
        // 添加消息到会话
        AgentMessage message = new AgentMessage("user", "Test message");
        manager.add(message, 1);
        
        // 验证消息已添加
        memory = manager.get(1);
        assertEquals(1, memory.size());
        assertEquals("Test message", memory.getLast().getContent());
    }
    
    @Test
    public void testAddMultipleMessages() {
        MemoryManager manager = new MemoryManager();
        
        // 创建消息列表
        AgentMessage message1 = new AgentMessage("user", "Message 1");
        AgentMessage message2 = new AgentMessage("assistant", "Message 2");
        List<AgentMessage> messages = Arrays.asList(message1, message2);
        
        // 添加消息列表到会话
        manager.add(messages, 1);
        
        // 验证消息已添加
        Memory memory = manager.get(1);
        assertEquals(2, memory.size());
    }
    
    @Test
    public void testClearSession() {
        MemoryManager manager = new MemoryManager();
        
        // 添加消息到会话
        AgentMessage message = new AgentMessage("user", "Test message");
        manager.add(message, 1);

        assertEquals(1, manager.get(1).size());
        // 清空会话
        manager.clear(1);
        
        // 验证会话已清空
        Memory memory = manager.get(1);
        assertTrue(memory.isEmpty());
    }
    
    @Test
    public void testClearAll() {
        MemoryManager manager = new MemoryManager();
        
        // 添加消息到多个会话
        AgentMessage message1 = new AgentMessage("user", "Message for session 1");
        AgentMessage message2 = new AgentMessage("user", "Message for session 2");
        manager.add(message1, 1);
        manager.add(message2, 2);
        
        // 清空所有会话
        manager.clearAll();
        
        // 验证所有会话已清空
        assertTrue(manager.get(1).isEmpty());
        assertTrue(manager.get(2).isEmpty());
    }
    
    @Test
    public void testRemoveSession() {
        MemoryManager manager = new MemoryManager();
        
        // 添加消息到会话
        AgentMessage message = new AgentMessage("user", "Test message");
        manager.add(message, 1);
        
        // 验证会话存在
        assertTrue(manager.containsSession(1));
        
        // 移除会话
        manager.remove(1);
        
        // 验证会话已移除
        assertFalse(manager.containsSession(1));
    }
    
    @Test
    public void testGetSessions() {
        MemoryManager manager = new MemoryManager();
        
        // 添加消息到多个会话
        AgentMessage message1 = new AgentMessage("user", "Message for session 1");
        AgentMessage message2 = new AgentMessage("user", "Message for session 2");
        manager.add(message1, 1);
        manager.add(message2, 2);
        
        // 获取所有会话ID
        List<Integer> sessions = manager.getSessions();
        assertEquals(2, sessions.size());
        assertTrue(sessions.contains(1));
        assertTrue(sessions.contains(2));
    }
    
    @Test
    public void testSessionCount() {
        MemoryManager manager = new MemoryManager();
        
        // 初始状态应该没有会话
        assertEquals(0, manager.sessionCount());
        
        // 添加消息到多个会话
        AgentMessage message1 = new AgentMessage("user", "Message for session 1");
        AgentMessage message2 = new AgentMessage("user", "Message for session 2");
        manager.add(message1, 1);
        manager.add(message2, 2);
        
        // 验证会话数量
        assertEquals(2, manager.sessionCount());
        
        // 移除一个会话
        manager.remove(1);
        
        // 验证会话数量减少
        assertEquals(1, manager.sessionCount());
    }
    
    @Test
    public void testWithCapacity() {
        // 创建带有容量限制的内存管理器
        Map<String, Object> config = new HashMap<>();
        config.put("capacity", 2);
        MemoryManager manager = new MemoryManager(config);
        
        // 添加3条消息到会话
        AgentMessage message1 = new AgentMessage("user", "Message 1");
        AgentMessage message2 = new AgentMessage("assistant", "Message 2");
        AgentMessage message3 = new AgentMessage("user", "Message 3");
        
        manager.add(message1, 1);
        manager.add(message2, 1);
        manager.add(message3, 1);
        
        // 验证内存大小仍为2
        Memory memory = manager.get(1);
        assertEquals(2, memory.size());
        
        // 验证最早的消息被移除
        List<AgentMessage> messages = memory.getMemory();
        assertEquals("Message 2", messages.get(0).getContent());
        assertEquals("Message 3", messages.get(1).getContent());
    }
} 