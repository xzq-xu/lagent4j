package io.github.internlm.lagent4j.examples;

import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.prompts.parsers.ToolParser;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.Map;

/**
 * 工具使用示例
 * <p>
 * 展示如何使用lagent4j框架创建一个能够调用工具的代理
 */
public class ToolExample {
    public static void main(String[] args) {

        String apiKey = System.getenv("LAGENT4J_MODEL_API_KEY");
        String baseUrl = System.getenv("LAGENT4J_MODEL_API_URL");
        String modelName = System.getenv("LAGENT4J_MODEL_NAME");

        // 创建OpenAI模型
        OpenAIModel llm = new OpenAIModel(apiKey,baseUrl,modelName);
        
        // 创建工具解析器
        ToolParser toolParser = new ToolParser("code_interpreter", "```python", "```");
        
        // 创建代理
        String systemPrompt = "你是一个有用的助手，能够编写Python代码解决问题。当需要编写代码时，请使用```python```代码块。";
        Agent agent = new Agent(llm, systemPrompt, null, toolParser, null, null, null);
        
        // 发送消息并获取回复
        String question = "请编写一个Python函数，计算斐波那契数列的第n项。";
        AgentMessage userMsg = new AgentMessage("user", question);
        AgentMessage botMsg = agent.process(userMsg);
        
        // 打印回复
        System.out.println("用户: " + userMsg.getContent());
        System.out.println("助手: " + botMsg.getContent());
        
        // 检查是否包含工具调用
        if (botMsg.getFormatted() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> formatted = (Map<String, Object>) botMsg.getFormatted();
            
            System.out.println("\n工具调用信息:");
            System.out.println("工具类型: " + formatted.get("tool_type"));
            System.out.println("思考过程: " + formatted.get("thought"));
            System.out.println("代码: " + formatted.get("action"));
            System.out.println("状态: " + formatted.get("status"));
            
            // 在实际应用中，这里可以执行代码并返回结果
            String codeExecutionResult = "执行结果:\n1, 1, 2, 3, 5, 8, 13, 21, 34, 55";
            
            // 将执行结果发送回代理
            AgentMessage executionMsg = new AgentMessage("code_executor", codeExecutionResult);
            AgentMessage finalMsg = agent.process(executionMsg);
            
            // 打印最终回复
            System.out.println("\n代码执行结果: " + codeExecutionResult);
            System.out.println("助手最终回复: " + finalMsg.getContent());
        }
    }
} 