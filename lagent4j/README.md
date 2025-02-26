# Lagent4j

一个基于Java的轻量级智能体框架，支持流式输出和工具调用。

## 项目状态

✅ 核心功能已完成：
- 基础Agent实现
- 内存管理
- LLM接口
- 工具调用
- 流式输出
- 解析器

✅ 示例代码已完成：
- 简单对话示例
- 工具调用示例
- 流式输出示例
- 多代理协作示例
- Web浏览器工具示例

✅ 测试已完成：
- 内存管理测试
- Agent测试
- 解析器测试

✅ 文档已完成：
- API文档
- 使用示例
- 贡献指南



## 简介

Lagent4j 是 [Lagent](https://github.com/InternLM/lagent) 的 Java 实现版本，提供了一个灵活、可扩展的大型语言模型（LLM）代理框架。该框架允许开发者轻松构建基于 LLM 的智能代理应用，支持多代理协作、工具调用和复杂任务处理。

Lagent4j 的设计理念借鉴了 PyTorch 的层级结构，使工作流程更加清晰直观。开发者只需专注于创建层和定义它们之间的消息传递，就能以 Java 风格构建复杂的代理应用。

## 特性

- **模型即代理**：将 LLM 封装为代理，简化交互流程
- **内存即状态**：自动管理代理的对话历史和状态
- **灵活的消息聚合**：支持自定义消息处理和格式化
- **响应格式化**：支持多种输出格式解析
- **工具调用一致性**：统一的工具调用接口和错误处理
- **多代理协作**：支持多个代理之间的协作和通信
- **异步支持**：提供异步 API 以支持高并发应用
- **流式响应**：支持流式输出，提升用户体验
- **钩子系统**：提供钩子机制，方便扩展和自定义行为

<!-- ## 安装

### Maven

```xml
<dependency>
    <groupId>io.github.internlm</groupId>
    <artifactId>lagent4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.internlm:lagent4j:1.0.0'
``` -->


## 快速开始

### 基本用法

```java
import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.schema.AgentMessage;

public class QuickStart {
    public static void main(String[] args) {
        // 创建LLM模型
        OpenAIModel llm = new OpenAIModel("your-api-key");
        
        // 创建代理
        String systemPrompt = "你是一个有用的助手。";
        Agent agent = new Agent(llm, systemPrompt);
        
        // 发送消息并获取回复
        AgentMessage userMsg = new AgentMessage("user", "今天天气怎么样？");
        AgentMessage botMsg = agent.process(userMsg);
        
        System.out.println(botMsg.getContent());
    }
}
```

### 使用工具

```java
import io.github.internlm.lagent4j.actions.ActionExecutor;
import io.github.internlm.lagent4j.actions.WebBrowser;
import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.prompts.parsers.JsonParser;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.Map;

public class WebBrowserExample {
    public static void main(String[] args) {
        // 创建LLM模型
        OpenAIModel llm = new OpenAIModel("your-api-key");
        
        // 创建JSON解析器
        JsonParser jsonParser = new JsonParser();
        
        // 创建代理
        String systemPrompt = "你是一个有用的助手，可以使用工具来回答问题。当你需要获取网页内容时，" +
                "你可以使用web_browser工具。请使用JSON格式回复，包含以下字段：\n" +
                "- action: 工具名称，如'web_browser'\n" +
                "- parameters: 工具参数，如{\"url\": \"https://example.com\"}\n" +
                "- thought: 你的思考过程";
        
        Agent agent = new Agent(llm, systemPrompt, null, jsonParser, null, null, null);
        
        // 创建工具执行器
        ActionExecutor executor = new ActionExecutor();
        executor.registerAction(new WebBrowser());
        
        // 发送消息并获取回复
        AgentMessage userMsg = new AgentMessage("user", "请获取OpenAI官网的首页内容并总结主要信息");
        AgentMessage botMsg = agent.process(userMsg);
        
        // 检查是否包含工具调用
        if (botMsg.getFormatted() != null) {
            // 执行工具调用
            AgentMessage toolMsg = executor.process(botMsg);
            
            // 将工具执行结果发送回代理
            AgentMessage finalMsg = agent.process(toolMsg);
            System.out.println(finalMsg.getContent());
        } else {
            System.out.println(botMsg.getContent());
        }
    }
}
```

### 流式输出

```java
import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class StreamExample {
    public static void main(String[] args) throws InterruptedException {
        // 创建LLM模型
        OpenAIModel llm = new OpenAIModel("your-api-key");
        
        // 创建代理
        String systemPrompt = "你是一个有用的助手，能够回答用户的各种问题。";
        Agent agent = new Agent(llm, systemPrompt);
        
        // 发送消息
        String question = "请详细介绍一下Java的历史和发展。";
        
        // 创建一个CountDownLatch来等待流式输出完成
        CountDownLatch latch = new CountDownLatch(1);
        
        // 存储完整的响应
        AtomicReference<String> fullResponse = new AtomicReference<>("");
        
        // 使用流式API
        llm.chatStream(
            // 聚合消息
            agent.getAggregator().aggregate(
                agent.getMemory().get(0), 
                agent.getName(), 
                agent.getOutputFormat(), 
                agent.getTemplate()
            ),
            // 处理每个文本块
            chunk -> {
                System.out.print(chunk);
                fullResponse.updateAndGet(current -> current + chunk);
            },
            // 处理错误
            error -> {
                System.err.println("\n发生错误: " + error.getMessage());
                latch.countDown();
            },
            // 处理完成
            () -> {
                System.out.println("\n流式输出完成");
                latch.countDown();
            }
        );
        
        // 等待流式输出完成
        latch.await();
    }
}
```

### 多代理协作

```java
import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.schema.AgentMessage;

public class MultiAgentExample {
    public static void main(String[] args) {
        // 创建LLM模型
        OpenAIModel llm = new OpenAIModel("your-api-key");
        
        // 创建多个代理
        Agent pythonExpert = new Agent(llm, 
                "你是一个Python专家，擅长编写高效、简洁的Python代码。", 
                null, null, null, "PythonExpert", "Python专家");
        
        Agent javaExpert = new Agent(llm, 
                "你是一个Java专家，擅长编写高效、简洁的Java代码。", 
                null, null, null, "JavaExpert", "Java专家");
        
        Agent moderator = new Agent(llm, 
                "你是一个讨论主持人，负责引导和总结讨论。", 
                null, null, null, "Moderator", "讨论主持人");
        
        // 用户问题
        String question = "我想了解Java和Python在处理大数据方面的区别。";
        
        // 第一步：主持人接收问题并分配任务
        AgentMessage userMsg = new AgentMessage("user", question);
        AgentMessage moderatorMsg = moderator.process(userMsg);
        
        // 第二步：向专家提问并获取回答
        // ...
        
        // 第三步：主持人总结讨论
        // ...
    }
}
```

### 使用钩子

```java
import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.hooks.HookManager;
import io.github.internlm.lagent4j.hooks.LoggingHook;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.schema.AgentMessage;

public class HookExample {
    public static void main(String[] args) {
        // 创建LLM模型
        OpenAIModel llm = new OpenAIModel("your-api-key");
        
        // 创建代理
        String systemPrompt = "你是一个有用的助手。";
        Agent agent = new Agent(llm, systemPrompt);
        
        // 创建钩子管理器
        HookManager hookManager = new HookManager();
        
        // 注册日志钩子
        hookManager.registerHook(new LoggingHook(true));
        
        // 发送消息并获取回复
        AgentMessage userMsg = new AgentMessage("user", "今天天气怎么样？");
        
        // 使用钩子处理消息
        List<AgentMessage> processedMessages = hookManager.executeBeforeAgent(agent, List.of(userMsg), 0);
        AgentMessage botMsg = agent.process(processedMessages.get(0));
        AgentMessage finalMsg = hookManager.executeAfterAgent(agent, botMsg, 0);
        
        System.out.println(finalMsg.getContent());
    }
}
```

## 架构

Lagent4j 的核心架构包括以下组件：

- **Agent**：代理是框架的基本单位，负责与LLM通信、管理内存和处理消息
- **LLM**：大型语言模型接口，支持多种模型实现
- **Memory**：内存管理器，存储和检索对话历史
- **Action**：工具和动作的抽象，支持代理调用外部功能
- **Schema**：核心数据结构，定义消息和状态的格式
- **Prompts**：提示模板和解析器，处理输入输出格式化
- **Hooks**：钩子系统，提供扩展点和自定义行为

## 高级用法

### 自定义聚合器

```java
import io.github.internlm.lagent4j.agents.aggregator.DefaultAggregator;
import io.github.internlm.lagent4j.memory.Memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FewshotAggregator extends DefaultAggregator {
    private final List<Map<String, String>> fewShot;
    
    public FewshotAggregator(List<Map<String, String>> fewShot) {
        this.fewShot = fewShot != null ? fewShot : new ArrayList<>();
    }
    
    @Override
    public List<Map<String, String>> aggregate(Memory memory, String name, 
                                              Object parser, Object systemInstruction) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 添加系统指令
        if (systemInstruction != null) {
            messages.addAll(aggregateSystemInstruction(systemInstruction));
        }
        
        // 添加少样本示例
        messages.addAll(fewShot);
        
        // 添加历史消息
        messages.addAll(aggregateMemory(memory, name));
        
        return messages;
    }
}
```

### 自定义工具

```java
import io.github.internlm.lagent4j.actions.Action;
import io.github.internlm.lagent4j.schema.ActionReturn;
import io.github.internlm.lagent4j.schema.ActionStatusCode;
import io.github.internlm.lagent4j.schema.FunctionCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Calculator implements Action {
    @Override
    public String getName() {
        return "calculator";
    }
    
    @Override
    public String getDescription() {
        return "一个简单的计算器，可以执行基本的数学运算";
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        
        Map<String, Object> expressionProperty = new HashMap<>();
        expressionProperty.put("type", "string");
        expressionProperty.put("description", "要计算的数学表达式");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("expression", expressionProperty);
        
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("expression"));
        
        return parameters;
    }
    
    @Override
    public ActionReturn execute(FunctionCall functionCall) {
        // 获取表达式参数
        String expression = null;
        if (functionCall.isParametersMap()) {
            Map<String, Object> params = functionCall.getParametersAsMap();
            if (params != null && params.containsKey("expression")) {
                expression = params.get("expression").toString();
            }
        } else {
            expression = functionCall.getParametersAsString();
        }
        
        if (expression == null || expression.isEmpty()) {
            return ActionReturn.builder()
                    .type(getName())
                    .state(ActionStatusCode.ARGS_ERROR)
                    .errmsg("Expression parameter is required")
                    .build();
        }
        
        try {
            // 使用JavaScript引擎计算表达式
            javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine = manager.getEngineByName("JavaScript");
            Object result = engine.eval(expression);
            
            // 构建结果
            List<Map<String, String>> resultList = new ArrayList<>();
            Map<String, String> item = new HashMap<>();
            item.put("type", "text");
            item.put("content", result.toString());
            resultList.add(item);
            
            return ActionReturn.builder()
                    .type(getName())
                    .state(ActionStatusCode.SUCCESS)
                    .result(resultList)
                    .build();
        } catch (Exception e) {
            return ActionReturn.builder()
                    .type(getName())
                    .state(ActionStatusCode.API_ERROR)
                    .errmsg("Error evaluating expression: " + e.getMessage())
                    .build();
        }
    }
}
```

## 贡献指南

我们欢迎各种形式的贡献，包括但不限于：

- 提交问题和功能请求
- 提交代码改进
- 改进文档
- 分享使用案例

### 运行测试

项目使用JUnit 5进行单元测试。要运行测试，可以使用以下命令：

```bash
mvn test
```

如果要运行特定的测试类，可以使用：

```bash
mvn test -Dtest=MemoryTest
```

### 代码风格

项目使用Google Java代码风格。请确保您的代码符合这一风格指南。

### 提交PR

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交变更 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

## 许可证

本项目采用 Apache 2.0 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

