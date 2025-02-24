package com.example.lagent4j.agent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActionResult {
    private Status status;
    private String output;
    private String error;

    public enum Status {
        SUCCESS,
        FAILURE
    }
}
