package io.github.internlm.lagent4j.examples;

import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 多代理协作示例
 * <p>
 * 展示如何使用多个代理进行协作
 */
public class MultiAgentExample {
    public static void main(String[] args) {
        String apiKey = System.getenv("LAGENT4J_MODEL_API_KEY");
        String baseUrl = System.getenv("LAGENT4J_MODEL_API_URL");
        String modelName = System.getenv("LAGENT4J_MODEL_NAME");

        // 创建OpenAI模型
        OpenAIModel llm = new OpenAIModel(apiKey,baseUrl,modelName);
        
        // 创建多个代理
        Agent pythonExpert = new Agent(llm, 
                "你是一个Python专家，擅长编写高效、简洁的Python代码。你的回答应该专注于Python的最佳实践和优化技巧。", 
                null, null, null, "PythonExpert", "Python专家");
        
        Agent javaExpert = new Agent(llm, 
                "你是一个Java专家，擅长编写高效、简洁的Java代码。你的回答应该专注于Java的最佳实践和优化技巧。", 
                null, null, null, "JavaExpert", "Java专家");
        
        Agent moderator = new Agent(llm, 
                "你是一个讨论主持人，负责引导和总结讨论。你需要根据用户的问题，向合适的专家提问，并总结他们的回答。", 
                null, null, null, "Moderator", "讨论主持人");
        
        // 用户问题
        String question = "我想了解Java和Python在处理大数据方面的区别，以及各自的优缺点。";
        
        // 开始多代理协作
        System.out.println("用户: " + question);
        
        // 第一步：主持人接收问题并分配任务
        AgentMessage userMsg = new AgentMessage("user", question);
        AgentMessage moderatorMsg = moderator.process(userMsg);
        
        System.out.println("\n主持人: " + moderatorMsg.getContent());
        
        // 第二步：向Python专家提问
        String pythonQuestion = "请详细介绍Python在处理大数据方面的优缺点和常用库。";
        AgentMessage toPythonExpert = new AgentMessage("Moderator", pythonQuestion);
        AgentMessage pythonExpertMsg = pythonExpert.process(toPythonExpert);
        
        System.out.println("\nPython专家: " + pythonExpertMsg.getContent());
        
        // 第三步：向Java专家提问
        String javaQuestion = "请详细介绍Java在处理大数据方面的优缺点和常用框架。";
        AgentMessage toJavaExpert = new AgentMessage("Moderator", javaQuestion);
        AgentMessage javaExpertMsg = javaExpert.process(toJavaExpert);
        
        System.out.println("\nJava专家: " + javaExpertMsg.getContent());
        
        // 第四步：主持人总结讨论
        List<AgentMessage> expertResponses = new ArrayList<>();
        expertResponses.add(new AgentMessage("PythonExpert", pythonExpertMsg.getContent()));
        expertResponses.add(new AgentMessage("JavaExpert", javaExpertMsg.getContent()));
        
        // 构建总结请求
        StringBuilder summaryRequest = new StringBuilder();
        summaryRequest.append("请总结以下专家关于Java和Python处理大数据的讨论：\n\n");
        summaryRequest.append("Python专家的回答：\n").append(pythonExpertMsg.getContent()).append("\n\n");
        summaryRequest.append("Java专家的回答：\n").append(javaExpertMsg.getContent());
        
        AgentMessage toModerator = new AgentMessage("user", summaryRequest.toString());
        AgentMessage finalSummary = moderator.process(toModerator);
        
        System.out.println("\n主持人总结: " + finalSummary.getContent());
    }
} 