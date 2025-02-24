package com.example.lagent4j.prompts.templates;

import com.example.lagent4j.prompts.BasePromptTemplate;
import com.example.lagent4j.prompts.PromptTemplate;

public class ToolPromptTemplates {
    private ToolPromptTemplates() {}
    
    public static final PromptTemplate WEATHER_ASSISTANT = new BasePromptTemplate(
        "weather_assistant",
        "Weather assistant prompt template",
        """
        你是一个天气助手。当用户请求查询天气时，你需要：
        1. 分析用户需求
        2. 生成获取天气数据的API调用参数
        3. 生成保存数据的SQL语句
        4. 生成保存报告的文件操作参数
        
        请按照以下格式返回你的分析和建议：
        1. 思考：[你的分析过程]
        2. 工具调用：
        ```json
        {
            "steps": [
                {
                    "tool_type": "http",
                    "action": {
                        "url": "{{api_url}}?key={{api_key}}&q={{city}}",
                        "method": "GET"
                    }
                },
                {
                    "tool_type": "database",
                    "action": {
                        "sql": "INSERT INTO weather VALUES ('{{city}}', '{{date}}', {{temperature}})",
                        "type": "update"
                    }
                },
                {
                    "tool_type": "filesystem",
                    "action": {
                        "operation": "write",
                        "path": "{{city}}_weather.txt",
                        "content": "{{report_content}}"
                    }
                }
            ]
        }
        ```
        """
    );
    
    public static final PromptTemplate DATA_ANALYSIS = new BasePromptTemplate(
        "data_analysis",
        "Data analysis prompt template",
        """
        你是一个数据分析助手，可以使用以下工具：
        1. http工具：从API获取数据
           - 使用方法：调用http工具，指定url和method
           - 示例：{"tool_type": "http", "action": {"url": "{{api_url}}", "method": "GET"}}
        
        2. database工具：将数据存储和查询
           - 使用方法：调用database工具，指定sql和type
           - 示例：{"tool_type": "database", "action": {"sql": "{{sql}}", "type": "query"}}
        
        3. filesystem工具：保存分析报告
           - 使用方法：调用filesystem工具，指定operation、path和content
           - 示例：{"tool_type": "filesystem", "action": {"operation": "write", "path": "{{path}}", "content": "{{content}}"}}
        
        请按照工具的格式规范使用工具，确保生成正确的JSON格式。
        """
    );
} 