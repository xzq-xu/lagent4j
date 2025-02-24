package com.example.lagent4j.examples;

import com.example.lagent4j.agent.*;
import com.example.lagent4j.actions.*;
import com.example.lagent4j.llm.*;
import com.example.lagent4j.prompts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 数据分析助手示例
 * 可以从API获取数据、存储到数据库、分析并生成报告
 */
public class DataAnalysisAssistant {
    private final AsyncStreamAgent agent;
    private final Logger logger = LoggerFactory.getLogger(DataAnalysisAssistant.class);

    public DataAnalysisAssistant(String apiKey) {
        // 初始化工具
        HttpRequestAction httpTool = new HttpRequestAction("http", 30);
        SimpleDbAction dbTool = new SimpleDbAction(
            "database",
            "jdbc:h2:mem:analysisdb;DB_CLOSE_DELAY=-1",
            "sa",
            "",
            5
        );
        FileIOAction fsTool = new FileIOAction(
            "filesystem",
            "./reports"
        );

        // 创建工具解析器
        JsonToolParser parser = new JsonToolParser(
            "analysis",
            "```json\n",
            "\n```"
        );

        // 初始化智能体
        agent = new AsyncStreamAgent(
            new AsyncOpenAILlmService(apiKey),
            new LRUMemory(100),
            List.of(httpTool, dbTool, fsTool),
            parser,
            """
            你是一个数据分析助手，可以使用以下工具：
            1. http工具：从API获取数据
               - 使用方法：调用http工具，指定url和method
               - 示例：{"tool_type": "http", "action": {"url": "https://api.example.com/data", "method": "GET"}}
            
            2. database工具：将数据存储和查询
               - 使用方法：调用database工具，指定sql和type
               - 示例：{"tool_type": "database", "action": {"sql": "SELECT * FROM data", "type": "query"}}
            
            3. filesystem工具：保存分析报告
               - 使用方法：调用filesystem工具，指定operation、path和content
               - 示例：{"tool_type": "filesystem", "action": {"operation": "write", "path": "report.txt", "content": "分析报告"}}
            
            请按照工具的格式规范使用工具，确保生成正确的JSON格式。
            """
        );

        // 设置回调
        agent.setOnMessage(msg -> {
            if (msg.getStreamState() == AgentMessage.StreamState.RUNNING) {
                System.out.print(msg.getContent());
            }
        });

        agent.setOnError(ex -> 
            logger.error("Error during analysis: ", ex)
        );

        // 初始化数据库表
        initDatabase();
    }

    private void initDatabase() {
        AgentMessage initMsg = AgentMessage.builder()
            .sender("system")
            .content("""
                使用database工具创建必要的数据表：
                1. CREATE TABLE IF NOT EXISTS stock_data (
                    symbol VARCHAR(10),
                    date DATE,
                    price DECIMAL(10,2),
                    volume BIGINT
                )
                2. CREATE TABLE IF NOT EXISTS analysis_results (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    analysis_date DATE,
                    symbol VARCHAR(10),
                    summary TEXT
                )
                """)
            .type(AgentMessage.AgentMessageType.SYSTEM)
            .sessionId(0)
            .build();

        try {
            agent.processAsync(initMsg, 0).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Failed to initialize database: ", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public CompletableFuture<String> analyzeStock(String symbol) {
        AgentMessage userMsg = AgentMessage.builder()
            .sender("user")
            .content(String.format("""
                请分析股票 %s 的数据：
                1. 从 Alpha Vantage API 获取股票数据
                2. 将数据保存到数据库
                3. 分析价格趋势和交易量
                4. 生成分析报告并保存到文件
                """, symbol))
            .type(AgentMessage.AgentMessageType.USER)
            .sessionId(1)
            .build();

        return agent.processAsync(userMsg, 1)
            .thenApply(response -> {
                logger.info("Analysis completed for symbol: {}", symbol);
                return response.getContent();
            })
            .exceptionally(ex -> {
                logger.error("Analysis failed for symbol: " + symbol, ex);
                return "分析失败: " + ex.getMessage();
            });
    }

    public CompletableFuture<String> generateReport(String symbol) {
        AgentMessage userMsg = AgentMessage.builder()
            .sender("user")
            .content(String.format("""
                请生成 %s 的分析报告：
                1. 从数据库查询分析结果
                2. 生成详细的分析报告
                3. 将报告保存为HTML文件
                """, symbol))
            .type(AgentMessage.AgentMessageType.USER)
            .sessionId(2)
            .build();

        return agent.processAsync(userMsg, 2)
            .thenApply(AgentMessage::getContent)
            .exceptionally(ex -> {
                logger.error("Report generation failed for symbol: " + symbol, ex);
                return "报告生成失败: " + ex.getMessage();
            });
    }

    public void close() {
        try {
            agent.reset(0);
            agent.reset(1);
            agent.reset(2);
        } catch (Exception e) {
            logger.error("Failed to cleanup resources: ", e);
        }
    }
} 