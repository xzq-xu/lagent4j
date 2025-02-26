package io.github.internlm.lagent4j.prompts.parsers;

import io.github.internlm.lagent4j.prompts.StrParser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具解析器
 * <p>
 * 用于解析模型输出中的工具调用部分
 */
public class ToolParser implements StrParser {
    /**
     * 工具类型
     */
    private final String toolType;
    
    /**
     * 开始标记
     */
    private final String begin;
    
    /**
     * 结束标记
     */
    private final String end;
    
    /**
     * 创建一个工具解析器
     *
     * @param toolType 工具类型
     * @param begin    开始标记
     * @param end      结束标记
     */
    public ToolParser(String toolType, String begin, String end) {
        this.toolType = toolType;
        this.begin = begin;
        this.end = end;
    }
    
    /**
     * 创建一个工具解析器，使用默认的开始和结束标记
     *
     * @param toolType 工具类型
     */
    public ToolParser(String toolType) {
        this(toolType, "```", "```");
    }
    
    @Override
    public Object parseResponse(String response) {
        Map<String, Object> result = new HashMap<>();
        result.put("tool_type", toolType);
        
        // 提取思考过程和动作
        String thought = "";
        String action = "";
        
        // 查找代码块
        Pattern pattern = Pattern.compile(Pattern.quote(begin) + "(.*?)" + Pattern.quote(end), Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            // 提取代码块前的内容作为思考过程
            int beginIndex = response.indexOf(begin);
            if (beginIndex > 0) {
                thought = response.substring(0, beginIndex).trim();
            }
            
            // 提取代码块内容作为动作
            action = matcher.group(1).trim();
            
            // 设置状态为有效
            result.put("status", 1);
        } else {
            // 如果没有找到代码块，则整个响应作为思考过程
            thought = response.trim();
            
            // 设置状态为无效
            result.put("status", 0);
        }
        
        result.put("thought", thought);
        result.put("action", action);
        
        return result;
    }
} 