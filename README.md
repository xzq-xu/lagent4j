# Lagent4j

一个基于Java的轻量级智能体框架，支持流式输出和工具调用。

## 特性

- 异步流式输出
- 工具调用支持
- 内存管理
- 可扩展的工具系统
- 完整的类型系统
- 简单易用的API

## 快速开始

### 安装

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>lagent4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基本使用

```java
// 创建天气助手
WeatherAssistant assistant = new WeatherAssistant("your-api-key");

// 查询天气
assistant.checkWeather("Shanghai")
    .thenAccept(result -> {
        System.out.println("天气信息：" + result);
    });
```

### 自定义工具

```java
// 创建自定义工具
HttpRequestAction httpTool = ToolFactory.createHttpTool();
SimpleDbAction dbTool = ToolFactory.createDatabaseTool(
    "jdbc:h2:mem:testdb",
    "sa",
    "",
    5
);

// 创建Agent
AsyncAgent agent = AgentBuilder.builder()
    .withLlm(new AsyncOpenAILlmService(apiKey))
    .withTools(List.of(httpTool, dbTool))
    .withOutputFormat(new JsonToolParser("test", "```json\n", "\n```"))
    .withSystemPrompt("You are a helpful assistant.")
    .build();
```

## 架构设计

### 核心组件

- Agent：智能体核心，负责协调LLM和工具
- Action：工具接口，定义了工具的基本操作
- Memory：会话记忆管理
- LLM Service：语言模型服务接口

### 工具系统

- HTTP工具：进行HTTP请求
- 数据库工具：执行SQL操作
- 文件系统工具：处理文件操作

## 开发指南

### 添加新工具

1. 实现 Action 接口
2. 实现 executeAsync 方法
3. 注册工具到 Agent

```java
public class MyTool implements Action {
    @Override
    public String getName() {
        return "my_tool";
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        // 实现工具逻辑
    }
}
```

### 自定义提示词模板

```java
public class MyPromptTemplate extends BasePromptTemplate {
    public MyPromptTemplate() {
        super(
            "my_template",
            "My custom template",
            """
            You are a helpful assistant.
            Please use the following tools:
            {{tools}}
            """
        );
    }
}
```

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交变更
4. 推送到分支
5. 创建 Pull Request

## 许可证

MIT License 