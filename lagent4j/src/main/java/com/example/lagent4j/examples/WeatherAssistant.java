package com.example.lagent4j.examples;

import com.example.lagent4j.agent.*;
import com.example.lagent4j.actions.*;
import com.example.lagent4j.llm.*;
import com.example.lagent4j.prompts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSONObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import com.example.lagent4j.prompts.templates.ToolPromptTemplates;
import com.example.lagent4j.actions.config.ToolConfig;
import com.example.lagent4j.actions.ToolFactory;

public class WeatherAssistant implements AutoCloseable {
    private final AsyncAgent agent;
    private final Logger logger = LoggerFactory.getLogger(WeatherAssistant.class);

    public WeatherAssistant(String apiKey) {
        // 使用工具工厂创建工具
        HttpRequestAction httpTool = ToolFactory.createHttpTool();
        SimpleDbAction dbTool = ToolFactory.createDatabaseTool(
            "jdbc:h2:mem:weatherdb;DB_CLOSE_DELAY=-1",
            "sa",
            "",
            5
        );
        FileIOAction fsTool = ToolFactory.createFileSystemTool("./weather_reports");

        // 创建工具解析器
        JsonToolParser parser = new JsonToolParser(
            "weather",
            "```json\n",
            "\n```"
        );

        // 使用工厂方法创建智能体
        Map<String, Object> promptParams = new HashMap<>();
        promptParams.put("api_url", "https://api.weatherapi.com/v1/current.json");
        promptParams.put("api_key", "YOUR_KEY");
        
        agent = AgentFactory.createStreamAgent(
            new AsyncOpenAILlmService(apiKey),
            List.of(httpTool, dbTool, fsTool),
            parser,
            ToolPromptTemplates.WEATHER_ASSISTANT.render(promptParams)
        );

        // 初始化数据库
        initDatabase();
    }

    private void initDatabase() {
        try {
            agent.getTools().stream()
                .filter(tool -> tool instanceof SimpleDbAction)
                .findFirst()
                .map(tool -> (SimpleDbAction) tool)
                .ifPresent(dbTool -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("sql", """
                        CREATE TABLE IF NOT EXISTS weather (
                            city VARCHAR(100),
                            date VARCHAR(10),
                            temperature DOUBLE,
                            PRIMARY KEY (city, date)
                        )
                        """);
                    params.put("type", "update");
                    dbTool.executeAsync(params).join();
                });
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public CompletableFuture<String> checkWeather(String city) {
        AgentMessage userMsg = AgentMessage.builder()
            .sender("user")
            .content(String.format("请帮我查询%s的天气情况，并生成天气报告。", city))
            .type(AgentMessage.AgentMessageType.USER)
            .sessionId(1)
            .build();

        return agent.processAsync(userMsg, 1)
            .thenCompose(response -> {
                // 解析LLM返回的工具调用建议
                JSONObject toolCalls = JSONObject.parseObject(
                    response.getContent().substring(
                        response.getContent().indexOf("```json\n") + 8,
                        response.getContent().lastIndexOf("\n```")
                    )
                );

                // 按顺序执行工具调用
                List<CompletableFuture<ActionResult>> futures = toolCalls.getJSONArray("steps")
                    .stream()
                    .map(step -> executeToolCall((JSONObject) step))
                    .toList();

                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.get(futures.size() - 1).join().getOutput());
            })
            .exceptionally(ex -> {
                logger.error("Weather check failed for city: " + city, ex);
                return "查询失败: " + ex.getMessage();
            });
    }

    private CompletableFuture<ActionResult> executeToolCall(JSONObject toolCall) {
        String toolType = toolCall.getString("tool_type");
        JSONObject action = toolCall.getJSONObject("action");
        
        // 将 JSONObject 转换为 Map
        Map<String, Object> params = new HashMap<>();
        action.forEach(params::put);
        
        return agent.getTools().stream()
            .filter(tool -> tool.getName().equals(toolType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolType))
            .executeAsync(params);
    }

    @Override
    public void close() {
        try {
            agent.reset(1);
        } catch (Exception e) {
            logger.error("Failed to cleanup resources", e);
        }
    }
}