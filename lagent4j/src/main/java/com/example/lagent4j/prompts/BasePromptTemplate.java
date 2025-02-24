package com.example.lagent4j.prompts;

import java.util.Map;
import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public class BasePromptTemplate implements PromptTemplate {
    private final String name;
    private final String description;
    private final String template;
    private final Pattern variablePattern = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*\\}\\}");

    public BasePromptTemplate(String name, String description, String template) {
        this.name = name;
        this.description = description;
        this.template = template;
    }

    @Override
    public String render(Map<String, Object> params) {
        String result = template;
        var matcher = variablePattern.matcher(template);
        
        while (matcher.find()) {
            String variable = matcher.group(1);
            Object value = params.get(variable);
            if (value != null) {
                result = result.replace("{{" + variable + "}}", value.toString());
            }
        }
        
        return result;
    }
} 