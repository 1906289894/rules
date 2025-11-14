package com.wb.rules.dto;

import com.wb.rules.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleExecutionResult {
    private boolean success;
    private String message;
    private Order data;
    
    public static RuleExecutionResult success(Order data) {
        return new RuleExecutionResult(true, "执行成功", data);
    }
    
    public static RuleExecutionResult error(String message) {
        return new RuleExecutionResult(false, message, null);
    }
}