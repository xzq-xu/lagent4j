package io.github.internlm.lagent4j.memory;

import io.github.internlm.lagent4j.schema.AgentMessage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Memory类的单元测试
 */
public class MemoryTest {

    @Test
    public void testAddAndGetMemory() {
        // 创建无限容量的内存
        Memory memory = new Memory();
        
        // 添加消息
        AgentMessage message1 = new AgentMessage("user", "Hello");
        AgentMessage message2 = new AgentMessage("assistant", "Hi there");
        
        memory.add(message1);
        memory.add(message2);
        
        // 验证内存大小
        assertEquals(2, memory.size());
        
        // 验证获取所有消息
        List<AgentMessage> messages = memory.getMemory();
        assertEquals(2, messages.size());
        assertEquals("Hello", messages.get(0).getContent());
        assertEquals("Hi there", messages.get(1).getContent());
    }
    
    @Test
    public void testAddMultipleMessages() {
        Memory memory = new Memory();
        
        // 创建消息列表
        AgentMessage message1 = new AgentMessage("user", "Hello");
        AgentMessage message2 = new AgentMessage("assistant", "Hi there");
        List<AgentMessage> messages = Arrays.asList(message1, message2);
        
        // 添加消息列表
        memory.add(messages);
        
        // 验证内存大小
        assertEquals(2, memory.size());
    }
    
    @Test
    public void testCapacity() {
        // 创建容量为2的内存
        Memory memory = new Memory(2);
        
        // 添加3条消息
        AgentMessage message1 = new AgentMessage("user", "Message 1");
        AgentMessage message2 = new AgentMessage("assistant", "Message 2");
        AgentMessage message3 = new AgentMessage("user", "Message 3");
        
        memory.add(message1);
        memory.add(message2);
        memory.add(message3);
        
        // 验证内存大小仍为2
        assertEquals(2, memory.size());
        
        // 验证最早的消息被移除
        List<AgentMessage> messages = memory.getMemory();
        assertEquals("Message 2", messages.get(0).getContent());
        assertEquals("Message 3", messages.get(1).getContent());
    }
    
    @Test
    public void testGetBySender() {
        Memory memory = new Memory();
        
        // 添加不同发送者的消息
        AgentMessage message1 = new AgentMessage("user", "User message 1");
        AgentMessage message2 = new AgentMessage("assistant", "Assistant message");
        AgentMessage message3 = new AgentMessage("user", "User message 2");
        
        memory.add(message1);
        memory.add(message2);
        memory.add(message3);
        
        // 获取用户消息
        List<AgentMessage> userMessages = memory.getBySender("user");
        assertEquals(2, userMessages.size());
        assertEquals("User message 1", userMessages.get(0).getContent());
        assertEquals("User message 2", userMessages.get(1).getContent());
        
        // 获取助手消息
        List<AgentMessage> assistantMessages = memory.getBySender("assistant");
        assertEquals(1, assistantMessages.size());
        assertEquals("Assistant message", assistantMessages.get(0).getContent());
    }
    
    @Test
    public void testGetLast() {
        Memory memory = new Memory();
        
        // 空内存时获取最后一条消息
        assertNull(memory.getLast());
        
        // 添加消息
        AgentMessage message1 = new AgentMessage("user", "First message");
        AgentMessage message2 = new AgentMessage("assistant", "Last message");
        
        memory.add(message1);
        memory.add(message2);
        
        // 获取最后一条消息
        AgentMessage lastMessage = memory.getLast();
        assertNotNull(lastMessage);
        assertEquals("Last message", lastMessage.getContent());
    }
    
    @Test
    public void testClear() {
        Memory memory = new Memory();
        
        // 添加消息
        AgentMessage message = new AgentMessage("user", "Test message");
        memory.add(message);
        
        // 验证内存不为空
        assertFalse(memory.isEmpty());
        
        // 清空内存
        memory.clear();
        
        // 验证内存为空
        assertTrue(memory.isEmpty());
        assertEquals(0, memory.size());
    }
} 