package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import org.junit.Before;
import org.junit.Test;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class HttpActionTest {
    private HttpAction httpAction;

    @Before
    public void setUp() {
        httpAction = new HttpAction("httpTest", 30);
    }

    @Test
    public void testGetRequest() throws Exception {
        Map<String, Object> params = Map.of(
            "url", "https://api.github.com/users/octocat",
            "method", "GET"
        );

        CompletableFuture<ActionResult> future = httpAction.executeAsync(params);
        ActionResult result = future.get(30, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(ActionResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("octocat"));
    }

    @Test
    public void testInvalidUrl() throws Exception {
        Map<String, Object> params = Map.of(
            "url", "https://invalid.domain.test",
            "method", "GET"
        );

        CompletableFuture<ActionResult> future = httpAction.executeAsync(params);
        ActionResult result = future.get(30, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(ActionResult.Status.FAILURE, result.getStatus());
        assertNotNull(result.getError());
    }

    @Test
    public void testPostRequest() throws Exception {
        Map<String, Object> params = Map.of(
            "url", "https://httpbin.org/post",
            "method", "POST",
            "body", "{\"test\":\"data\"}",
            "headers", Map.of("Content-Type", "application/json")
        );

        CompletableFuture<ActionResult> future = httpAction.executeAsync(params);
        ActionResult result = future.get(30, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(ActionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getOutput().contains("\"test\":\"data\""));
    }
} 