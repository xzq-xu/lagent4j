package com.example.lagent4j.prompts;

import java.util.Map;

public interface PromptTemplate {
    String render(Map<String, Object> params);
    String getName();
    String getDescription();
} 