package com.example.lagent4j.integration;

import com.alibaba.fastjson2.JSONObject;
import com.example.lagent4j.agent.*;
import com.example.lagent4j.actions.*;
import com.example.lagent4j.llm.*;
import com.example.lagent4j.prompts.*;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.*;

public class AgentIntegrationTest {
    private TestableAsyncStreamAgent agent;
    private List<AgentMessage> receivedMessages;
    private List<String> toolResults;

    @Before
    public void setUp() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable not set");
        }

        // 初始化测试工具
        MockHttpAction httpTool = new MockHttpAction();
        MockDbAction dbTool = new MockDbAction();
        MockFileAction fileTool = new MockFileAction();

        // 初始化Agent
        agent = new TestableAsyncStreamAgent(
            new AsyncOpenAILlmService(apiKey),
            new LRUMemory(100),
            List.of(httpTool, dbTool, fileTool),
            new JsonToolParser("test", "```json\n", "\n```"),
            "You are a helpful assistant that can use tools."
        );

        // 记录接收到的消息
        receivedMessages = new ArrayList<>();
        toolResults = new ArrayList<>();
        agent.setOnMessage(msg -> receivedMessages.add(msg));
    }

    @Test
    public void testBasicConversation() throws Exception {
        AgentMessage userMsg = AgentMessage.builder()
            .sender("user")
            .content("Hello!")
            .type(AgentMessage.AgentMessageType.USER)
            .sessionId(1)
            .build();

        AgentMessage response = agent.processAsync(userMsg, 1)
            .get(30, TimeUnit.SECONDS);

        assertNotNull(response);
        assertEquals(AgentMessage.AgentMessageType.ASSISTANT, response.getType());
        assertTrue(response.getContent().length() > 0);
    }

    @Test
    public void testToolUsage() throws Exception {
        AgentMessage userMsg = AgentMessage.builder()
            .sender("user")
            .content("Get data from https://api.example.com")
            .type(AgentMessage.AgentMessageType.USER)
            .sessionId(1)
            .build();

        AgentMessage response = agent.processAsync(userMsg, 1)
            .get(30, TimeUnit.SECONDS);

        assertNotNull(response);
        assertNotNull(response.getFormatted());
        assertTrue(response.getFormatted() instanceof JSONObject);
    }

    @Test
    public void testStreamOutput() throws Exception {
        AgentMessage userMsg = AgentMessage.builder()
            .sender("user")
            .content("Write a short story")
            .type(AgentMessage.AgentMessageType.USER)
            .sessionId(1)
            .build();

        agent.processAsync(userMsg, 1).get(30, TimeUnit.SECONDS);

        assertTrue(receivedMessages.size() > 1);
        assertTrue(receivedMessages.stream()
            .anyMatch(msg -> msg.getStreamState() == AgentMessage.StreamState.RUNNING));
    }

    @Test
    public void testMemoryManagement() throws Exception {
        // 发送多条消息
        for (int i = 0; i < 3; i++) {
            AgentMessage userMsg = AgentMessage.builder()
                .sender("user")
                .content("Message " + i)
                .type(AgentMessage.AgentMessageType.USER)
                .sessionId(1)
                .build();

            agent.processAsync(userMsg, 1).get(30, TimeUnit.SECONDS);
        }

        // 检查历史消息
        List<AgentMessage> history = agent.getMemory().get(1);
        assertTrue(history.size() >= 6); // 系统提示 + 3对对话
        
        // 测试重置
        agent.reset(1);
        history = agent.getMemory().get(1);
        assertTrue(history.isEmpty());
    }

    // Mock工具类
    private static class MockHttpAction implements Action {
        @Override
        public String getName() {
            return "http";
        }

        @Override
        public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
            return CompletableFuture.completedFuture(
                ActionResult.builder()
                    .status(ActionResult.Status.SUCCESS)
                    .output("{\"data\": \"mock response\"}")
                    .build()
            );
        }
    }

    private static class MockDbAction implements Action {
        @Override
        public String getName() {
            return "database";
        }

        @Override
        public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
            return CompletableFuture.completedFuture(
                ActionResult.builder()
                    .status(ActionResult.Status.SUCCESS)
                    .output("Query executed successfully")
                    .build()
            );
        }
    }

    private static class MockFileAction implements Action {
        @Override
        public String getName() {
            return "filesystem";
        }

        @Override
        public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
            return CompletableFuture.completedFuture(
                ActionResult.builder()
                    .status(ActionResult.Status.SUCCESS)
                    .output("File operation completed")
                    .build()
            );
        }
    }

    // 添加可测试的Agent子类
    private static class TestableAsyncStreamAgent extends AsyncStreamAgent {
        public TestableAsyncStreamAgent(
                AsyncLlmService llm,
                Memory memory,
                List<Action> actions,
                OutputFormat outputFormat,
                String systemPrompt) {
            super(llm, memory, actions, outputFormat, systemPrompt);
        }

        public Memory getMemory() {
//            return memory;
            return null;
        }
    }
} 