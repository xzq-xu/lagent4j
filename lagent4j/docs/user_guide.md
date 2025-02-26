# Lagent4j 用户指南

## 目录

- [快速开始](#快速开始)
- [核心概念](#核心概念)
- [高级用法](#高级用法)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

## 快速开始

### 安装

将以下依赖添加到您的Maven项目中：

```xml
<dependency>
    <groupId>io.github.internlm</groupId>
    <artifactId>lagent4j</artifactId>
    <version>0.1.0</version>
</dependency>
```

或者在Gradle项目中：

```groovy
implementation 'io.github.internlm:lagent4j:0.1.0'
```

### 基本配置

首先，创建一个语言模型实例：

```java
import io.github.internlm.lagent4j.llms.OpenAILLM;
import io.github.internlm.lagent4j.llms.BaseLLM;

// 创建OpenAI模型实例
BaseLLM llm = new OpenAILLM(
    System.getenv("LAGENT4J_MODEL_API_KEY"),  // API密钥
    System.getenv("LAGENT4J_MODEL_API_URL"),  // API基础URL（可选）
    System.getenv("LAGENT4J_MODEL_NAME")      // 模型名称，如"gpt-3.5-turbo"
);
```

### 创建一个简单的Agent

```java
import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.message.AgentMessage;

// 创建一个简单的Agent
Agent agent = new Agent(llm, "你是一个有用的助手。");

// 处理用户消息
AgentMessage response = agent.process("你好，请介绍一下自己。");
System.out.println(response.getContent());
```

### 使用工具

```java
import io.github.internlm.lagent4j.actions.Action;
import io.github.internlm.lagent4j.actions.ActionExecutor;
import io.github.internlm.lagent4j.actions.WebBrowser;

// 创建工具执行器
ActionExecutor executor = new ActionExecutor();

// 注册网页浏览器工具
executor.registerAction(new WebBrowser());

// 创建Agent并处理消息
Agent agent = new Agent(llm, "你是一个有用的助手，可以使用工具来回答问题。");
AgentMessage userMessage = new AgentMessage("user", "请查询一下今天的天气。");

// 处理消息并执行工具调用
AgentMessage response = agent.process(userMessage);
response = executor.process(response);
response = agent.process(response);

System.out.println(response.getContent());
```

## 核心概念

### 消息系统

Lagent4j使用`AgentMessage`类来表示消息：

```java
import io.github.internlm.lagent4j.message.AgentMessage;

// 创建用户消息
AgentMessage userMessage = new AgentMessage("user", "你好，请帮我查询天气。");

// 创建带有格式化内容的消息
AgentMessage formattedMessage = new AgentMessage("assistant", "这是内容", formattedObject);
```

### 工具调用

工具（Actions）是Agent可以调用的外部功能：

```java
import io.github.internlm.lagent4j.actions.Action;
import io.github.internlm.lagent4j.actions.ActionReturn;
import io.github.internlm.lagent4j.message.FunctionCall;

// 自定义工具实现
public class Calculator implements Action {
    @Override
    public String getName() {
        return "calculator";
    }
    
    @Override
    public String getDescription() {
        return "执行基本的数学计算";
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        // 定义参数schema
        return params;
    }
    
    @Override
    public ActionReturn execute(FunctionCall functionCall) {
        // 实现计算逻辑
        Map<String, Object> params = functionCall.getParametersAsMap();
        // 处理参数并返回结果
        ActionReturn result = new ActionReturn();
        // 设置结果
        return result;
    }
}
```

### 输出格式化

Lagent4j支持自定义输出格式解析：

```java
import io.github.internlm.lagent4j.output.StrParser;

// 自定义输出解析器
public class JsonParser implements StrParser {
    @Override
    public Object parseResponse(String response) {
        // 解析JSON响应
        return parseJson(response);
    }
    
    private Object parseJson(String json) {
        // 实现JSON解析逻辑
        return null;
    }
}
```

## 高级用法

### 流式处理

处理流式输出：

```java
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// 流式处理回调
Consumer<String> onChunk = chunk -> System.out.println("收到块: " + chunk);
Consumer<Throwable> onError = error -> System.err.println("错误: " + error.getMessage());
Runnable onDone = () -> System.out.println("流式处理完成");

// 流式聊天
List<Map<String, String>> messages = List.of(
    Map.of("role", "user", "content", "请生成一个长故事")
);
llm.chatStream(messages, onChunk, onError, onDone);
```

### 会话管理

管理多个会话：

```java
import io.github.internlm.lagent4j.memory.MemoryManager;

// 创建内存管理器
MemoryManager memoryManager = new MemoryManager();

// 创建Agent
Agent agent = new Agent(llm, "你是一个有用的助手。");

// 处理不同会话的消息
int session1 = 1;
int session2 = 2;

// 会话1
AgentMessage response1 = agent.process("你好，我是Alice", session1);
System.out.println(response1.getContent());

// 会话2
AgentMessage response2 = agent.process("你好，我是Bob", session2);
System.out.println(response2.getContent());

// 继续会话1
AgentMessage response3 = agent.process("你还记得我是谁吗？", session1);
System.out.println(response3.getContent());  // 应该记得是Alice
```

### 错误处理

处理可能的异常：

```java
try {
    AgentMessage response = agent.process("请执行一个复杂的任务");
    System.out.println(response.getContent());
} catch (Exception e) {
    System.err.println("处理消息时出错: " + e.getMessage());
    e.printStackTrace();
}
```

## 最佳实践

### 资源管理

合理管理内存和会话：

```java
// 设置内存容量限制
MemoryManager memoryManager = new MemoryManager(100);  // 每个会话最多保存100条消息

// 不再需要时清理会话
memoryManager.clear(sessionId);

// 完全不需要时移除会话
memoryManager.remove(sessionId);
```

### 异步处理

对于耗时操作，使用异步方法：

```java
import java.util.concurrent.CompletableFuture;

// 异步聊天
CompletableFuture<String> future = llm.chatAsync(messages);

// 添加回调
future.thenAccept(response -> {
    System.out.println("收到响应: " + response);
}).exceptionally(error -> {
    System.err.println("发生错误: " + error.getMessage());
    return null;
});
```

### 超时控制

为长时间运行的操作添加超时控制：

```java
import java.util.concurrent.TimeUnit;

try {
    // 设置5秒超时
    String response = llm.chatAsync(messages)
        .get(5, TimeUnit.SECONDS);
    System.out.println(response);
} catch (TimeoutException e) {
    System.err.println("操作超时");
}
```

## 常见问题

### 工具调用超时

**问题**: 工具调用执行时间过长导致超时。

**解决方案**: 对于耗时的工具操作，考虑实现异步执行并设置合理的超时：

```java
@Override
public ActionReturn execute(FunctionCall functionCall) {
    ActionReturn result = new ActionReturn();
    
    try {
        // 设置超时执行
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            // 执行耗时操作
            return computeResult();
        });
        
        Object computedResult = future.get(30, TimeUnit.SECONDS);
        // 设置结果
    } catch (TimeoutException e) {
        // 处理超时
        result.setErrmsg("操作超时");
    } catch (Exception e) {
        // 处理其他异常
        result.setErrmsg("执行出错: " + e.getMessage());
    }
    
    return result;
}
```

### 自定义内存管理

**问题**: 需要自定义内存管理策略。

**解决方案**: 使用`Memory`类的API来实现自定义内存管理：

```java
import io.github.internlm.lagent4j.memory.Memory;

// 创建自定义内存
Memory memory = new Memory(50);  // 最多保存50条消息

// 添加消息
memory.add(new AgentMessage("user", "你好"));

// 获取特定发送者的消息
List<AgentMessage> userMessages = memory.getBySender("user");

// 获取最后一条消息
AgentMessage lastMessage = memory.getLast();

// 清空内存
memory.clear();
``` 