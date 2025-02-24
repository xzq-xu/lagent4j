package com.example.lagent4j.actions;

import com.example.lagent4j.actions.config.ToolConfig;
import com.example.lagent4j.constants.ToolConstants;
import java.util.List;

public class ToolFactory {
    private ToolFactory() {}
    
    public static List<Action> createDefaultTools() {
        return List.of(
            createHttpTool(),
            createDatabaseTool(),
            createFileSystemTool()
        );
    }
    
    public static HttpRequestAction createHttpTool() {
        return new HttpRequestAction(
            ToolConstants.HTTP_TOOL,
            30
        );
    }
    
    public static SimpleDbAction createDatabaseTool() {
        return new SimpleDbAction(
            ToolConstants.DATABASE_TOOL,
            "jdbc:h2:mem:defaultdb;DB_CLOSE_DELAY=-1",
            "sa",
            "",
            5
        );
    }
    
    public static FileIOAction createFileSystemTool() {
        return new FileIOAction(
            ToolConstants.FILESYSTEM_TOOL,
            "./output"
        );
    }
    
    public static HttpRequestAction createHttpTool(ToolConfig config) {
        return new HttpRequestAction(
            config.getName(),
            config.getTimeout()
        );
    }
    
    public static SimpleDbAction createDatabaseTool(
            String jdbcUrl,
            String username,
            String password,
            int poolSize) {
        return new SimpleDbAction(
            ToolConstants.DATABASE_TOOL,
            jdbcUrl,
            username,
            password,
            poolSize
        );
    }
    
    public static FileIOAction createFileSystemTool(String rootPath) {
        return new FileIOAction(
            ToolConstants.FILESYSTEM_TOOL,
            rootPath
        );
    }
} 