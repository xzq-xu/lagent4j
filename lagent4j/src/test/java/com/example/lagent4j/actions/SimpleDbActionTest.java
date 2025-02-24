package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class SimpleDbActionTest {
    private SimpleDbAction dbAction;

    @Before
    public void setUp() {
        dbAction = new SimpleDbAction(
            "test_db",
            "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
            "sa",
            "",
            5
        );

        // 创建测试表
        Map<String, Object> params = new HashMap<>();
        params.put("sql", """
            CREATE TABLE test (
                id INT PRIMARY KEY,
                name VARCHAR(100)
            )
            """);
        params.put("type", "update");
        
        try {
            dbAction.executeAsync(params).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Failed to create test table: " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        if (dbAction != null) {
            dbAction.close();
        }
    }

    @Test
    public void testInsertAndQuery() throws Exception {
        // 插入测试数据
        Map<String, Object> insertParams = new HashMap<>();
        insertParams.put("sql", "INSERT INTO test VALUES (1, 'test')");
        insertParams.put("type", "update");
        
        ActionResult insertResult = dbAction.executeAsync(insertParams)
            .get(5, TimeUnit.SECONDS);
        assertEquals(ActionResult.Status.SUCCESS, insertResult.getStatus());

        // 查询测试数据
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("sql", "SELECT * FROM test WHERE id = 1");
        queryParams.put("type", "query");
        
        ActionResult queryResult = dbAction.executeAsync(queryParams)
            .get(5, TimeUnit.SECONDS);
        assertEquals(ActionResult.Status.SUCCESS, queryResult.getStatus());
        assertTrue(queryResult.getOutput().contains("test"));
    }

    @Test
    public void testInvalidSql() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sql", "INVALID SQL");
        params.put("type", "query");
        
        ActionResult result = dbAction.executeAsync(params)
            .get(5, TimeUnit.SECONDS);
        assertEquals(ActionResult.Status.FAILURE, result.getStatus());
        assertNotNull(result.getError());
    }
} 