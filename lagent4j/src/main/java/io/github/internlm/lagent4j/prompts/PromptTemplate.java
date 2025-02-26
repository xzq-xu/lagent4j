package io.github.internlm.lagent4j.prompts;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示模板类
 * <p>
 * 用于处理提示模板，支持变量替换
 */
public class PromptTemplate {
    /**
     * 模板文本
     */
    private final String template;
    
    /**
     * 变量模式，匹配{variable_name}格式的变量
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^{}]+)\\}");
    
    /**
     * 创建一个提示模板
     *
     * @param template 模板文本
     */
    public PromptTemplate(String template) {
        this.template = template;
    }
    
    /**
     * 使用提供的变量填充模板
     *
     * @param variables 变量映射
     * @return 填充后的文本
     */
    public String format(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = value != null ? value.toString() : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * 使用提供的变量填充模板
     *
     * @param variables 变量值，按顺序对应模板中的变量
     * @return 填充后的文本
     */
    public String format(Object... variables) {
        if (variables == null || variables.length == 0) {
            return template;
        }
        
        Map<String, Object> variableMap = new HashMap<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        int index = 0;
        
        while (matcher.find() && index < variables.length) {
            String variableName = matcher.group(1);
            variableMap.put(variableName, variables[index++]);
        }
        
        return format(variableMap);
    }
    
    /**
     * 获取模板文本
     *
     * @return 模板文本
     */
    public String getTemplate() {
        return template;
    }
    
    @Override
    public String toString() {
        return template;
    }
} 