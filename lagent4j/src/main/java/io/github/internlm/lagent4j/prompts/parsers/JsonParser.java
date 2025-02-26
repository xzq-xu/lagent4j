package io.github.internlm.lagent4j.prompts.parsers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.internlm.lagent4j.prompts.StrParser;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON解析器
 * <p>
 * 用于解析模型输出中的JSON格式数据
 */
@Slf4j
public class JsonParser implements StrParser {
    /**
     * JSON开始标记
     */
    private final String begin;
    
    /**
     * JSON结束标记
     */
    private final String end;
    
    /**
     * 创建一个JSON解析器
     *
     * @param begin 开始标记
     * @param end   结束标记
     */
    public JsonParser(String begin, String end) {
        this.begin = begin;
        this.end = end;
    }
    
    /**
     * 创建一个JSON解析器，使用默认的开始和结束标记
     */
    public JsonParser() {
        this("```json", "```");
    }
    
    @Override
    public Object parseResponse(String response) {
        Map<String, Object> result = new HashMap<>();
        
        // 提取思考过程和JSON数据
        String thought = "";
        String jsonStr = "";
        
        // 查找JSON块
        Pattern pattern = Pattern.compile(Pattern.quote(begin) + "(.*?)" + Pattern.quote(end), Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            // 提取JSON块前的内容作为思考过程
            int beginIndex = response.indexOf(begin);
            if (beginIndex > 0) {
                thought = response.substring(0, beginIndex).trim();
            }
            
            // 提取JSON块内容
            jsonStr = matcher.group(1).trim();
            
            try {
                // 解析JSON
                JSONObject jsonObject = JSON.parseObject(jsonStr);
                
                // 将JSON对象的所有键值对添加到结果中
                for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                    result.put(entry.getKey(), entry.getValue());
                }
                
                // 设置状态为有效
                result.put("status", 1);
            } catch (Exception e) {
                log.error("Failed to parse JSON: {}", jsonStr, e);
                
                // 设置状态为无效
                result.put("status", 0);
                result.put("error", e.getMessage());
            }
        } else {
            // 如果没有找到JSON块，尝试直接解析整个响应
            try {
                JSONObject jsonObject = JSON.parseObject(response);
                
                // 将JSON对象的所有键值对添加到结果中
                for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                    result.put(entry.getKey(), entry.getValue());
                }
                
                // 设置状态为有效
                result.put("status", 1);
            } catch (Exception e) {
                // 如果直接解析也失败，则整个响应作为思考过程
                thought = response.trim();
                
                // 设置状态为无效
                result.put("status", 0);
                result.put("error", e.getMessage());
            }
        }
        
        // 添加思考过程
        if (!thought.isEmpty()) {
            result.put("thought", thought);
        }
        
        return result;
    }
} 