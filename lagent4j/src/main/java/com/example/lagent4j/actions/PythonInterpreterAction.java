package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PythonInterpreterAction implements Action {
    
    @Override
    public String getName() {
        return "python_interpreter";
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String code = (String) parameters.get("code");
                ProcessBuilder pb = new ProcessBuilder("python", "-c", code);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return ActionResult.builder()
                        .status(ActionResult.Status.SUCCESS)
                        .output(output.toString())
                        .build();
                } else {
                    return ActionResult.builder()
                        .status(ActionResult.Status.FAILURE)
                        .error("Process exited with code " + exitCode)
                        .build();
                }
            } catch (Exception e) {
                return ActionResult.builder()
                    .status(ActionResult.Status.FAILURE)
                    .error(e.getMessage())
                    .build();
            }
        });
    }
}
