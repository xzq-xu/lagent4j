package com.example.lagent4j.agent;

import com.example.lagent4j.actions.Action;
import com.example.lagent4j.llm.AsyncLlmService;
import com.example.lagent4j.prompts.OutputFormat;
import java.util.List;

/**
 * Agent构建器
 * 用于简化Agent的创建过程
 */
public class AgentBuilder {
    private AsyncLlmService llm;
    private Memory memory;
    private List<Action> tools;
    private OutputFormat outputFormat;
    private String systemPrompt;
    private AgentConfig config;

    public AgentBuilder withLlm(AsyncLlmService llm) {
        this.llm = llm;
        return this;
    }

    public AgentBuilder withMemory(Memory memory) {
        this.memory = memory;
        return this;
    }

    public AgentBuilder withTools(List<Action> tools) {
        this.tools = tools;
        return this;
    }

    public AgentBuilder withOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
        return this;
    }

    public AgentBuilder withSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    public AgentBuilder withConfig(AgentConfig config) {
        this.config = config;
        return this;
    }

    public AsyncAgent build() {
        if (llm == null) {
            throw new IllegalStateException("LLM service is required");
        }
        if (memory == null) {
            memory = new LRUMemory(100);
        }
        if (tools == null) {
            throw new IllegalStateException("Tools are required");
        }
        if (outputFormat == null) {
            throw new IllegalStateException("Output format is required");
        }
        if (systemPrompt == null) {
            systemPrompt = "You are a helpful assistant.";
        }
        if (config == null) {
            config = AgentConfig.getDefault();
        }

        return new AsyncStreamAgent(llm, memory, tools, outputFormat, systemPrompt);
    }
} 