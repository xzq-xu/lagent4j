package com.example.lagent4j.agent;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolCall {
    private String toolName;
    private Map<String, Object> parameters;
}
