package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import com.alibaba.fastjson2.JSONObject;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 数据库操作工具
 * 支持SQL查询和更新操作
 */
public class DatabaseAction implements Action {
    private final DataSource dataSource;
    private final String name;

    public DatabaseAction(String name, String jdbcUrl, String username, String password) {
        this.name = name;
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = (String) parameters.get("sql");
            String type = (String) parameters.getOrDefault("type", "query");
            
            try (Connection conn = dataSource.getConnection()) {
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
            
            JSONObject result = new JSONObject();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                JSONObject row = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                result.put("row" + rs.getRow(), row);
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
                .output(String.format("Affected rows: %d", affected))
                .build();
        }
    }
} 