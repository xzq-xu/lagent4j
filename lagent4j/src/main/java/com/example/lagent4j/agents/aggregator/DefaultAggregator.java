package com.example.lagent4j.agents.aggregator;

import com.example.lagent4j.agent.AgentMessage;
import com.example.lagent4j.agent.Memory;
import java.util.*;

public class DefaultAggregator implements MessageAggregator {
    private final String agentName;
    private final Memory memory;

    public DefaultAggregator(String agentName, Memory memory) {
        this.agentName = agentName;
        this.memory = memory;
    }

    @Override
    public List<Map<String, String>> aggregateMessages(long sessionId) {
        List<Map<String, String>> aggregated = new ArrayList<>();
        
        // 处理历史消息
        for (AgentMessage message : memory.get(sessionId)) {
            Map<String, String> formattedMessage = new HashMap<>();
            
            if (message.getSender().equals(agentName)) {
                formattedMessage.put("role", "assistant");
            } else {
                formattedMessage.put("role", message.getSender());
            }
            
            formattedMessage.put("content", message.getContent());
            aggregated.add(formattedMessage);
        }
        
        return aggregated;
    }
} 