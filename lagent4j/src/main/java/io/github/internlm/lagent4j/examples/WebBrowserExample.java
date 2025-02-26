package io.github.internlm.lagent4j.examples;

import io.github.internlm.lagent4j.actions.ActionExecutor;
import io.github.internlm.lagent4j.actions.WebBrowser;
import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.prompts.parsers.JsonParser;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.Map;

/**
 * WebBrowser工具使用示例
 * <p>
 * 展示如何使用WebBrowser工具获取网页内容
 */
public class WebBrowserExample {
    public static void main(String[] args) {
        // 替换为你的OpenAI API密钥

        String apiKey = System.getenv("LAGENT4J_MODEL_API_KEY");
        String baseUrl = System.getenv("LAGENT4J_MODEL_API_URL");
        String modelName = System.getenv("LAGENT4J_MODEL_NAME");

        // 创建OpenAI模型
        OpenAIModel llm = new OpenAIModel(apiKey,baseUrl,modelName);
        
        // 创建JSON解析器
        JsonParser jsonParser = new JsonParser();
        
        // 创建代理
        String systemPrompt = "你是一个有用的助手，可以使用工具来回答问题。当你需要获取网页内容时，" +
                "你可以使用web_browser工具。请使用JSON格式回复，包含以下字段：\n" +
                "- action: 工具名称，如'web_browser'\n" +
                "- parameters: 工具参数，如{\"url\": \"https://example.com\"}\n" +
                "- thought: 你的思考过程\n\n" +
                "示例：\n```json\n" +
                "{\n" +
                "  \"thought\": \"我需要获取网页内容来回答这个问题\",\n" +
                "  \"action\": \"web_browser\",\n" +
                "  \"parameters\": {\"url\": \"https://example.com\"}\n" +
                "}\n```";
        
        Agent agent = new Agent(llm, systemPrompt, null, jsonParser, null, null, null);
        
        // 创建工具执行器
        ActionExecutor executor = new ActionExecutor();
        executor.registerAction(new WebBrowser());
        
        // 发送消息并获取回复
        String question = "请浏览CSDN获取关于MCP协议相关的信息";
        AgentMessage userMsg = new AgentMessage("user", question);
        AgentMessage botMsg = agent.process(userMsg);
        
        // 打印回复
        System.out.println("用户: " + question);
        System.out.println("助手: " + botMsg.getContent());
        
        // 检查是否包含工具调用
        if (botMsg.getFormatted() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> formatted = (Map<String, Object>) botMsg.getFormatted();
            
            System.out.println("\n工具调用信息:");
            System.out.println("思考过程: " + formatted.get("thought"));
            System.out.println("工具: " + formatted.get("action"));
            System.out.println("参数: " + formatted.get("parameters"));
            
            // 执行工具调用
            AgentMessage toolMsg = executor.process(botMsg);
            
            System.out.println("\n工具执行结果:");
            System.out.println(toolMsg.getContent());
            
            // 将工具执行结果发送回代理
            AgentMessage finalMsg = agent.process(toolMsg);
            
            System.out.println("\n助手最终回复:");
            System.out.println(finalMsg.getContent());
        }
    }
} 