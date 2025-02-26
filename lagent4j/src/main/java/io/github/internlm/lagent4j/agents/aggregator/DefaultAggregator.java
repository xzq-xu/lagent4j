package io.github.internlm.lagent4j.agents.aggregator;

import io.github.internlm.lagent4j.memory.Memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认消息聚合器
 * <p>
 * 用于将内存中的消息聚合为模型可接受的格式
 */
public class DefaultAggregator {
    /**
     * 聚合消息
     *
     * @param memory            内存
     * @param name              代理名称
     * @param parser            输出解析器
     * @param systemInstruction 系统指令
     * @return 聚合后的消息列表
     */
    public List<Map<String, String>> aggregate(Memory memory, String name, 
                                              Object parser, Object systemInstruction) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 添加系统指令
        if (systemInstruction != null) {
            messages.addAll(aggregateSystemInstruction(systemInstruction));
        }
        
        // 添加历史消息
        if (memory != null) {
            messages.addAll(aggregateMemory(memory, name));
        }
        
        return messages;
    }
    
    /**
     * 聚合系统指令
     *
     * @param systemInstruction 系统指令
     * @return 聚合后的系统指令消息列表
     */
    public List<Map<String, String>> aggregateSystemInstruction(Object systemInstruction) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        if (systemInstruction instanceof String) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", (String) systemInstruction);
            messages.add(systemMessage);
        } else if (systemInstruction instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> systemMessage = (Map<String, String>) systemInstruction;
            messages.add(systemMessage);
        } else if (systemInstruction instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> systemMessages = (List<Map<String, String>>) systemInstruction;
            messages.addAll(systemMessages);
        }
        
        return messages;
    }
    
    /**
     * 聚合内存中的消息
     *
     * @param memory 内存
     * @param name   代理名称
     * @return 聚合后的历史消息列表
     */
    protected List<Map<String, String>> aggregateMemory(Memory memory, String name) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        memory.getMemory().forEach(message -> {
            Map<String, String> formattedMessage = new HashMap<>();
            
            if (message.getSender().equals(name)) {
                formattedMessage.put("role", "assistant");
            } else {
                formattedMessage.put("role", "user");
            }
            
            formattedMessage.put("content", message.getContent().toString());
            messages.add(formattedMessage);
        });
        
        return messages;
    }
} 