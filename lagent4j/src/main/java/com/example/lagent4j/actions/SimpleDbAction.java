package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import com.example.lagent4j.constants.ToolConstants;
import com.example.lagent4j.exceptions.ToolException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 简单数据库操作工具
 * 使用基础连接池实现
 */
public class SimpleDbAction implements Action, AutoCloseable {
    private final String name;
    private final HikariDataSource dataSource;

    public SimpleDbAction(String name, String jdbcUrl, String username, String password, int poolSize) {
        this.name = name;
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                String sql = (String) parameters.get(ToolConstants.SQL_KEY);
                String type = (String) parameters.getOrDefault(ToolConstants.TYPE_KEY, "query");
                
                if ("query".equalsIgnoreCase(type)) {
                    return executeQuery(conn, sql);
                } else {
                    return executeUpdate(conn, sql);
                }
            } catch (Exception e) {
                return ActionResult.builder()
                    .status(ActionResult.Status.FAILURE)
                    .error(e.getMessage())
                    .build();
            }
        });
    }

    private ActionResult executeQuery(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            StringBuilder result = new StringBuilder();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    result.append(metaData.getColumnName(i))
                          .append(": ")
                          .append(rs.getString(i))
                          .append(", ");
                }
                result.append("\n");
            }
            
            return ActionResult.builder()
                .status(ActionResult.Status.SUCCESS)
                .output(result.toString())
                .build();
        }
    }

    private ActionResult executeUpdate(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            int affected = stmt.executeUpdate(sql);
            return ActionResult.builder()
                .status(ActionResult.Status.SUCCESS)
                .output("Affected rows: " + affected)
                .build();
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
} 