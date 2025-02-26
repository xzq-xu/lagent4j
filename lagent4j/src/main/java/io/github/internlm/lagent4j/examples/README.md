# Lagent4j 示例程序

本目录包含了多个示例程序，展示如何使用Lagent4j框架的各种功能。这些示例可以帮助您快速上手并理解框架的核心概念。

## 环境变量设置

所有示例程序都需要设置以下环境变量：

- `LAGENT4J_MODEL_API_KEY`: 您的API密钥
- `LAGENT4J_MODEL_API_URL`: API的基础URL（可选，默认为OpenAI的URL）
- `LAGENT4J_MODEL_NAME`: 使用的模型名称（如"gpt-3.5-turbo"）

## 示例程序列表

### SimpleExample.java

这是一个基础示例，展示了如何创建一个简单的对话代理，发送消息并获取回复。该示例还演示了如何维护对话上下文以及如何重置对话。

主要功能：
- 创建基本的Agent实例
- 发送用户消息并获取回复
- 维护对话上下文
- 重置对话历史

### ToolExample.java

这个示例展示了如何使用工具解析器创建一个能够调用工具的代理。在这个例子中，代理被配置为生成Python代码，并且可以处理代码执行结果。

主要功能：
- 使用ToolParser解析模型输出中的代码块
- 检测和提取工具调用信息
- 处理工具执行结果

### StreamExample.java

这个示例演示了如何使用流式输出功能，实现实时生成和显示模型响应。

主要功能：
- 使用同步和流式两种方式获取模型响应
- 处理流式输出的文本块
- 处理流式输出的错误和完成事件
- 更新代理内存以维护对话上下文

### WebBrowserExample.java

这个示例展示了如何使用WebBrowser工具获取网页内容，并让代理处理这些信息。

主要功能：
- 创建和注册WebBrowser工具
- 使用JsonParser解析模型输出中的JSON格式数据
- 执行工具调用并处理结果
- 将工具执行结果发送回代理进行进一步处理

### MultiAgentExample.java

这个示例演示了如何创建多个专家代理和一个主持人代理，实现多代理协作解决复杂问题。

主要功能：
- 创建具有不同专业知识的多个代理
- 实现代理之间的消息传递
- 使用主持人代理协调讨论
- 汇总多个专家的回答

## 运行示例

您可以通过以下方式运行示例程序：

```bash
# 设置环境变量
export LAGENT4J_MODEL_API_KEY="your_api_key"
export LAGENT4J_MODEL_API_URL="https://api.openai.com/v1"  # 可选
export LAGENT4J_MODEL_NAME="gpt-3.5-turbo"

# 运行示例
java -cp lagent4j.jar io.github.internlm.lagent4j.examples.SimpleExample
```

或者在Windows上：

```batch
set LAGENT4J_MODEL_API_KEY=your_api_key
set LAGENT4J_MODEL_API_URL=https://api.openai.com/v1
set LAGENT4J_MODEL_NAME=gpt-3.5-turbo

java -cp lagent4j.jar io.github.internlm.lagent4j.examples.SimpleExample
```

## 自定义示例

您可以基于这些示例创建自己的应用程序。关键步骤包括：

1. 创建适合您需求的LLM实例
2. 配置适当的系统提示和解析器
3. 创建Agent实例
4. 根据需要注册工具
5. 处理用户输入并显示代理响应

更多详细信息，请参考项目的主README文件和API文档。 