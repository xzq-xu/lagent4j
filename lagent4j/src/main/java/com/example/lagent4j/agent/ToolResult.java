package com.example.lagent4j.agent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolResult {
    private String toolName;
    private ActionResult result;
}
