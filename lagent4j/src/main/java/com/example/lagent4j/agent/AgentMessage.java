package com.example.lagent4j.agent;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class AgentMessage {
    private String sender;
    private String content;
    private Object formatted;
    private Map<String, Object> extraInfo;
    private AgentMessageType type;
    private Integer sessionId;
    
    // Legacy fields
    private String toolCallId;
    private String toolName;
    private String toolArgs;

    public enum AgentMessageType {
        USER, AGENT, TOOL, SYSTEM
    }
}
