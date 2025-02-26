package io.github.internlm.lagent4j.actions;

import io.github.internlm.lagent4j.schema.ActionReturn;
import io.github.internlm.lagent4j.schema.ActionStatusCode;
import io.github.internlm.lagent4j.schema.AgentMessage;
import io.github.internlm.lagent4j.schema.FunctionCall;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动作执行器
 * <p>
 * 用于管理和执行工具
 */
@Slf4j
public class ActionExecutor {
    /**
     * 动作映射表
     */
    private final Map<String, Action> actions;
    
    /**
     * 创建一个动作执行器
     */
    public ActionExecutor() {
        this.actions = new ConcurrentHashMap<>();
    }
    
    /**
     * 注册动作
     *
     * @param action 动作
     */
    public void registerAction(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }
        
        String name = action.getName();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Action name cannot be null or empty");
        }
        
        actions.put(name, action);
        log.debug("Registered action: {}", name);
    }
    
    /**
     * 注销动作
     *
     * @param name 动作名称
     * @return 是否成功注销
     */
    public boolean unregisterAction(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        Action removed = actions.remove(name);
        if (removed != null) {
            log.debug("Unregistered action: {}", name);
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取所有动作的描述
     *
     * @return 动作描述列表
     */
    public List<Map<String, Object>> getActionDescriptions() {
        List<Map<String, Object>> descriptions = new ArrayList<>();
        
        for (Action action : actions.values()) {
            Map<String, Object> description = new HashMap<>();
            description.put("name", action.getName());
            description.put("description", action.getDescription());
            description.put("parameters", action.getParameters());
            descriptions.add(description);
        }
        
        return descriptions;
    }
    
    /**
     * 处理代理消息
     *
     * @param message 代理消息
     * @return 处理结果
     */
    @SuppressWarnings("unchecked")
    public AgentMessage process(AgentMessage message) {
        if (message == null || message.getFormatted() == null) {
            return new AgentMessage("action_executor", "No action to execute");
        }
        
        Map<String, Object> formatted = (Map<String, Object>) message.getFormatted();
        
        // 提取函数调用信息
        String actionName = null;
        Map<String, Object> parameters = null;
        
        if (formatted.containsKey("function_call")) {
            Map<String, Object> functionCall = (Map<String, Object>) formatted.get("function_call");
            actionName = (String) functionCall.get("name");
            parameters = (Map<String, Object>) functionCall.get("arguments");
        } else if (formatted.containsKey("action")) {
            actionName = (String) formatted.get("action");
            parameters = (Map<String, Object>) formatted.get("parameters");
        }
        
        if (actionName == null) {
            return new AgentMessage("action_executor", "No action specified");
        }
        
        // 查找动作
        Action action = actions.get(actionName);
        if (action == null) {
            log.warn("Action not found: {}", actionName);
            return new AgentMessage("action_executor", "Action not found: " + actionName);
        }
        
        // 执行动作
        try {
            FunctionCall functionCall = new FunctionCall(actionName, parameters);
            ActionReturn result = action.execute(functionCall);
            
            // 构建响应消息
            String content = result.formatResult();
            if (content.isEmpty()) {
                content = "Action executed successfully, but no result was returned.";
            }
            
            return new AgentMessage("action_executor", content);
        } catch (Exception e) {
            log.error("Error executing action: {}", actionName, e);
            
            // 构建错误响应
            ActionReturn errorResult = ActionReturn.builder()
                    .type(actionName)
                    .state(ActionStatusCode.API_ERROR)
                    .errmsg("Error executing action: " + e.getMessage())
                    .build();
            
            return new AgentMessage("action_executor", errorResult.getErrmsg());
        }
    }
    
    /**
     * 获取动作数量
     *
     * @return 动作数量
     */
    public int getActionCount() {
        return actions.size();
    }
    
    /**
     * 清空所有动作
     */
    public void clearActions() {
        actions.clear();
        log.debug("Cleared all actions");
    }
} 