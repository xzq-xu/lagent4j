package io.github.internlm.lagent4j.memory;

import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存管理器类
 * <p>
 * 用于管理多个会话的内存，每个会话由一个唯一的会话ID标识
 */
public class MemoryManager {
    /**
     * 存储会话ID到内存的映射
     */
    private final Map<Integer, Memory> memories;
    
    /**
     * 内存容量，-1表示无限制
     */
    private final int capacity;
    
    /**
     * 创建一个无容量限制的内存管理器
     */
    public MemoryManager() {
        this(-1);
    }
    
    /**
     * 创建一个指定容量的内存管理器
     *
     * @param capacity 内存容量，-1表示无限制
     */
    public MemoryManager(int capacity) {
        this.memories = new HashMap<>();
        this.capacity = capacity;
    }
    
    /**
     * 创建一个使用指定内存类型的内存管理器
     *
     * @param memoryConfig 内存配置
     */
    public MemoryManager(Map<String, Object> memoryConfig) {
        this.memories = new HashMap<>();
        
        // 从配置中获取容量，默认为-1（无限制）
        this.capacity = memoryConfig.containsKey("capacity") ? 
                (int) memoryConfig.get("capacity") : -1;
    }
    
    /**
     * 获取指定会话的内存，如果不存在则创建
     *
     * @param sessionId 会话ID
     * @return 指定会话的内存
     */
    public Memory get(int sessionId) {
        if (!memories.containsKey(sessionId)) {
            memories.put(sessionId, new Memory(capacity));
        }
        return memories.get(sessionId);
    }
    
    /**
     * 添加消息到指定会话的内存
     *
     * @param message   要添加的消息
     * @param sessionId 会话ID
     */
    public void add(AgentMessage message, int sessionId) {
        get(sessionId).add(message);
    }
    
    /**
     * 添加多条消息到指定会话的内存
     *
     * @param messages  要添加的消息列表
     * @param sessionId 会话ID
     */
    public void add(List<AgentMessage> messages, int sessionId) {
        get(sessionId).add(messages);
    }
    
    /**
     * 清空指定会话的内存
     *
     * @param sessionId 会话ID
     */
    public void clear(int sessionId) {
        if (memories.containsKey(sessionId)) {
            memories.get(sessionId).clear();
        }
    }
    
    /**
     * 清空所有会话的内存
     */
    public void clearAll() {
        for (Memory memory : memories.values()) {
            memory.clear();
        }
    }
    
    /**
     * 移除指定会话的内存
     *
     * @param sessionId 会话ID
     */
    public void remove(int sessionId) {
        memories.remove(sessionId);
    }
    
    /**
     * 检查指定会话是否存在
     *
     * @param sessionId 会话ID
     * @return 如果会话存在则返回true，否则返回false
     */
    public boolean containsSession(int sessionId) {
        return memories.containsKey(sessionId);
    }
    
    /**
     * 获取所有会话ID
     *
     * @return 所有会话ID的集合
     */
    public List<Integer> getSessions() {
        return List.copyOf(memories.keySet());
    }
    
    /**
     * 获取会话数量
     *
     * @return 会话数量
     */
    public int sessionCount() {
        return memories.size();
    }
} 