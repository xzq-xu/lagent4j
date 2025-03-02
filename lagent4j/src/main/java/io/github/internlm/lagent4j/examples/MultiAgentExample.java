package io.github.internlm.lagent4j.examples;

import io.github.internlm.lagent4j.actions.Action;
import io.github.internlm.lagent4j.actions.ActionExecutor;
import io.github.internlm.lagent4j.actions.WebBrowser;
import io.github.internlm.lagent4j.actions.WebSearch;
import io.github.internlm.lagent4j.agents.SyncAgent;
import io.github.internlm.lagent4j.agents.aggregator.DefaultAggregator;
import io.github.internlm.lagent4j.llms.OpenAIModel;
import io.github.internlm.lagent4j.prompts.parsers.JsonParser;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.HashMap;

/**
 * 多代理协作示例
 * <p>
 * 展示如何使用多个专业代理进行协作，共同解决复杂问题。
 * 每个代理都可以使用工具，并且可以相互交流。
 */
public class MultiAgentExample {
    public static void main(String[] args) {
        // 检查环境变量
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("请设置OPENAI_API_KEY环境变量");
            return;
        }

        // 创建OpenAI模型
        OpenAIModel model = new OpenAIModel.Builder()
                .apiKey(apiKey)
                .model("gpt-3.5-turbo")
                .temperature(0.7)
                .maxTokens(2000)
                .build();

        // 创建工具执行器
        ActionExecutor executor = new ActionExecutor();
        
        // 注册工具
        Action webBrowser = new WebBrowser();
        Action webSearch = new WebSearch();
        executor.registerAction(webBrowser);
        executor.registerAction(webSearch);

        // 创建Python专家代理
        String pythonTemplate = """
            你是一个资深的Python技术专家，在Python编程和架构设计方面有超过10年的经验。
            
            在回答问题时，请遵循以下原则：
            1. 仔细分析问题，判断是否需要使用工具
            2. 如果需要搜索资料，使用web_search工具
            3. 如果需要查看具体内容，使用web_browser工具
            4. 提供具体的代码示例和最佳实践
            5. 解释Python的独特优势和适用场景
            6. 分享实际项目中的经验教训
            7. 推荐合适的库和工具
            8. 用中文回答所有问题
            """;

        SyncAgent pythonExpert = new SyncAgent(
                model,
                pythonTemplate,
                new HashMap<>(),
                new JsonParser(),
                new DefaultAggregator(),
                executor,
                "Python专家",
                "资深Python技术专家"
        );

        // 创建Java专家代理
        String javaTemplate = """
            你是一个资深的Java技术专家，在Java编程和架构设计方面有超过10年的经验。
            
            在回答问题时，请遵循以下原则：
            1. 仔细分析问题，判断是否需要使用工具
            2. 如果需要搜索资料，使用web_search工具
            3. 如果需要查看具体内容，使用web_browser工具
            4. 提供具体的代码示例和最佳实践
            5. 解释Java的独特优势和适用场景
            6. 分享实际项目中的经验教训
            7. 推荐合适的框架和工具
            8. 用中文回答所有问题
            """;

        SyncAgent javaExpert = new SyncAgent(
                model,
                javaTemplate,
                new HashMap<>(),
                new JsonParser(),
                new DefaultAggregator(),
                executor,
                "Java专家",
                "资深Java技术专家"
        );

        // 创建主持人代理
        String moderatorTemplate = """
            你是一个经验丰富的技术讨论主持人，擅长引导和总结技术讨论。
            
            在主持讨论时，请遵循以下原则：
            1. 仔细分析问题，判断是否需要使用工具
            2. 如果需要搜索资料，使用web_search工具
            3. 如果需要查看具体内容，使用web_browser工具
            4. 清晰地分配问题给相关专家
            5. 确保讨论围绕主题进行
            6. 总结各方观点的异同
            7. 提炼出实用的结论
            8. 用中文主持讨论
            """;

        SyncAgent moderator = new SyncAgent(
                model,
                moderatorTemplate,
                new HashMap<>(),
                new JsonParser(),
                new DefaultAggregator(),
                executor,
                "主持人",
                "技术讨论主持人"
        );

        // 演示多个场景的多代理协作

        // 场景1：技术选型讨论
        System.out.println("=== 场景1：技术选型讨论 ===");
        AgentMessage question1 = new AgentMessage(
                "user",
                "我们正在开发一个大数据处理系统，需要同时处理实时数据和批量数据。请讨论在Java和Python之间应该如何选择，以及是否可以混合使用。"
        );
        processMultiAgentDiscussion(moderator, pythonExpert, javaExpert, question1);

        // 场景2：性能优化建议
        System.out.println("\n=== 场景2：性能优化建议 ===");
        AgentMessage question2 = new AgentMessage(
                "user",
                "我们的数据处理系统在处理大量数据时性能不佳，请分析可能的原因并给出优化建议。需要考虑多线程、内存管理、算法优化等方面。"
        );
        processMultiAgentDiscussion(moderator, pythonExpert, javaExpert, question2);

        // 场景3：最新技术趋势
        System.out.println("\n=== 场景3：最新技术趋势 ===");
        AgentMessage question3 = new AgentMessage(
                "user",
                "请讨论Java和Python在大数据和AI领域的最新发展趋势，以及各自的优势和不足。"
        );
        processMultiAgentDiscussion(moderator, pythonExpert, javaExpert, question3);
    }

    /**
     * 处理多代理讨论
     *
     * @param moderator    主持人代理
     * @param pythonExpert Python专家代理
     * @param javaExpert   Java专家代理
     * @param question     用户问题
     */
    private static void processMultiAgentDiscussion(
            SyncAgent moderator,
            SyncAgent pythonExpert,
            SyncAgent javaExpert,
            AgentMessage question) {
        System.out.println("用户: " + question.getContent() + "\n");

        // 第一步：主持人分析问题并分配任务
        System.out.println("主持人正在分析问题...\n");
        AgentMessage moderatorMsg = moderator.process(question, 1);
        System.out.println("主持人: " + moderatorMsg.getContent() + "\n");

        // 第二步：Python专家回答
        System.out.println("Python专家正在思考...\n");
        AgentMessage pythonQuestion = new AgentMessage(
                "主持人",
                "请从Python的角度详细分析这个问题，包括技术特点、最佳实践、实际案例等。如果需要，可以搜索相关资料或查看具体实现。"
        );
        AgentMessage pythonAnswer = pythonExpert.process(pythonQuestion, 1);
        System.out.println("Python专家: " + pythonAnswer.getContent() + "\n");

        // 第三步：Java专家回答
        System.out.println("Java专家正在思考...\n");
        AgentMessage javaQuestion = new AgentMessage(
                "主持人",
                "请从Java的角度详细分析这个问题，包括技术特点、最佳实践、实际案例等。如果需要，可以搜索相关资料或查看具体实现。"
        );
        AgentMessage javaAnswer = javaExpert.process(javaQuestion, 1);
        System.out.println("Java专家: " + javaAnswer.getContent() + "\n");

        // 第四步：主持人总结讨论
        System.out.println("主持人正在总结讨论...\n");
        StringBuilder summaryRequest = new StringBuilder();
        summaryRequest.append("请总结Python专家和Java专家的讨论要点：\n\n");
        summaryRequest.append("Python专家观点：\n").append(pythonAnswer.getContent()).append("\n\n");
        summaryRequest.append("Java专家观点：\n").append(javaAnswer.getContent());

        AgentMessage summaryQuestion = new AgentMessage("user", summaryRequest.toString());
        AgentMessage summary = moderator.process(summaryQuestion, 1);
        System.out.println("主持人总结: " + summary.getContent() + "\n");
        
        System.out.println("-------------------\n");
    }
} 