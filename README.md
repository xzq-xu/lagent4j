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
- 使用指南
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

## 安装

### Maven

```xml
<dependency>
    <groupId>io.github.internlm</groupId>
    <artifactId>lagent4j</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.internlm:lagent4j:0.1.0'
```

## 快速开始

### 基本用法

```java
import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAILLM;
import io.github.internlm.lagent4j.llms.BaseLLM;
import io.github.internlm.lagent4j.message.AgentMessage;

public class QuickStart {
    public static void main(String[] args) {
        // 创建LLM模型
        BaseLLM llm = new OpenAILLM(
            System.getenv("LAGENT4J_MODEL_API_KEY"),
            System.getenv("LAGENT4J_MODEL_API_URL"),
            System.getenv("LAGENT4J_MODEL_NAME")
        );
        
        // 创建代理
        Agent agent = new Agent(llm, "你是一个有用的助手。");
        
        // 发送消息并获取回复
        AgentMessage response = agent.process("今天天气怎么样？");
        
        System.out.println(response.getContent());
    }
}
```

### 使用工具

```java
import io.github.internlm.lagent4j.actions.Action;
import io.github.internlm.lagent4j.actions.ActionExecutor;
import io.github.internlm.lagent4j.actions.WebBrowser;
import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAILLM;
import io.github.internlm.lagent4j.llms.BaseLLM;
import io.github.internlm.lagent4j.message.AgentMessage;

public class ToolExample {
    public static void main(String[] args) {
        // 创建LLM模型
        BaseLLM llm = new OpenAILLM(
            System.getenv("LAGENT4J_MODEL_API_KEY"),
            System.getenv("LAGENT4J_MODEL_API_URL"),
            System.getenv("LAGENT4J_MODEL_NAME")
        );
        
        // 创建代理
        Agent agent = new Agent(llm, "你是一个有用的助手，可以使用工具来回答问题。");
        
        // 创建工具执行器
        ActionExecutor executor = new ActionExecutor();
        executor.registerAction(new WebBrowser());
        
        // 发送消息并获取回复
        AgentMessage userMessage = new AgentMessage("user", "请查询一下今天的天气。");
        
        // 处理消息并执行工具调用
        AgentMessage response = agent.process(userMessage);
        response = executor.process(response);
        response = agent.process(response);
        
        System.out.println(response.getContent());
    }
}
```

### 流式输出

```java
import io.github.internlm.lagent4j.llms.OpenAILLM;
import io.github.internlm.lagent4j.llms.BaseLLM;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class StreamExample {
    public static void main(String[] args) {
        // 创建LLM模型
        BaseLLM llm = new OpenAILLM(
            System.getenv("LAGENT4J_MODEL_API_KEY"),
            System.getenv("LAGENT4J_MODEL_API_URL"),
            System.getenv("LAGENT4J_MODEL_NAME")
        );
        
        // 流式处理回调
        Consumer<String> onChunk = chunk -> System.out.print(chunk);
        Consumer<Throwable> onError = error -> System.err.println("\n错误: " + error.getMessage());
        Runnable onDone = () -> System.out.println("\n流式输出完成");
        
        // 流式聊天
        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", "请生成一个长故事")
        );
        
        llm.chatStream(messages, onChunk, onError, onDone);
    }
}
```

## 文档

详细文档请参考以下资源：

- [API参考文档](docs/api_reference.md)：详细的API说明和用法
- [用户指南](docs/user_guide.md)：全面的使用教程和最佳实践
- [示例程序](src/main/java/io/github/internlm/lagent4j/examples/README.md)：各种使用场景的示例代码

## 运行测试

本项目使用JUnit 5进行单元测试。您可以通过以下命令运行测试：

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=MemoryTest
```

Windows用户可以使用提供的批处理脚本：

```cmd
run_tests.bat
```

Linux/Mac用户可以使用Shell脚本：

```bash
./run_tests.sh
```

## 代码风格

本项目遵循Google Java代码风格。请确保您的代码符合这一规范。

## 提交PR

如果您想为项目做出贡献，请按照以下步骤操作：

1. Fork本项目
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建一个Pull Request

## 提交到GitHub

为了方便将项目提交到GitHub，我们提供了自动化脚本：

### Windows用户

运行以下批处理脚本：

```cmd
github_submit.bat
```

此脚本将引导您完成以下步骤：
- 初始化Git仓库（如果尚未初始化）
- 配置远程仓库
- 添加文件到暂存区
- 创建提交
- 推送到GitHub

### Linux/Mac用户

运行以下Shell脚本：

```bash
# 先添加执行权限
chmod +x github_submit.sh
# 运行脚本
./github_submit.sh
```

此脚本提供与Windows版本相同的功能，帮助您轻松将项目提交到GitHub。

## 许可证

本项目采用 Apache 2.0 许可证。详情请参见 [LICENSE](LICENSE) 文件。

## 联系我们

如有任何问题或建议，请通过以下方式联系我们：

- 提交 [Issue](https://github.com/InternLM/lagent4j/issues)
- 发送邮件至 [support@internlm.org](mailto:support@internlm.org)

## 致谢

感谢所有为本项目做出贡献的开发者和用户。

