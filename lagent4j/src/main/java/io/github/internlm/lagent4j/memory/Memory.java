package io.github.internlm.lagent4j.memory;

import io.github.internlm.lagent4j.schema.AgentMessage;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 内存类
 * <p>
 * 用于存储和管理代理的对话历史
 */
public class Memory {
    /**
     * 存储消息的列表
     */
    private final List<AgentMessage> memory;
    
    /**
     * 最大内存容量，-1表示无限制
     */
    private final int capacity;
    
    /**
     * 创建一个无容量限制的内存
     */
    public Memory() {
        this(-1);
    }
    
    /**
     * 创建一个指定容量的内存
     *
     * @param capacity 内存容量，-1表示无限制
     */
    public Memory(int capacity) {
        this.memory = new ArrayList<>();
        this.capacity = capacity;
    }
    
    /**
     * 添加一条消息到内存
     *
     * @param message 要添加的消息
     */
    public void add(AgentMessage message) {
        if (capacity > 0 && memory.size() >= capacity) {
            memory.remove(0);
        }
        memory.add(message);
    }
    
    /**
     * 添加多条消息到内存
     *
     * @param messages 要添加的消息列表
     */
    public void add(List<AgentMessage> messages) {
        for (AgentMessage message : messages) {
            add(message);
        }
    }
    
    /**
     * 清空内存
     */
    public void clear() {
        memory.clear();
    }
    
    /**
     * 获取内存中的所有消息
     *
     * @return 内存中的所有消息
     */
    public List<AgentMessage> getMemory() {
        return new ArrayList<>(memory);
    }
    
    /**
     * 获取指定发送者的消息
     *
     * @param sender 发送者名称
     * @return 指定发送者的所有消息
     */
    public List<AgentMessage> getBySender(String sender) {
        return memory.stream()
                .filter(message -> message.getSender().equals(sender))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取最后一条消息
     *
     * @return 最后一条消息，如果内存为空则返回null
     */
    public AgentMessage getLast() {
        if (memory.isEmpty()) {
            return null;
        }
        return memory.get(memory.size() - 1);
    }
    
    /**
     * 获取内存大小
     *
     * @return 内存中消息的数量
     */
    public int size() {
        return memory.size();
    }
    
    /**
     * 检查内存是否为空
     *
     * @return 如果内存为空则返回true，否则返回false
     */
    public boolean isEmpty() {
        return memory.isEmpty();
    }
} 