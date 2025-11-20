package com.wb.rules.common.exceptions;

import com.wb.rules.common.enums.RuleResultCode;
import lombok.Getter;

import java.io.Serial;

@Getter
public class RuleException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private final int code;
    
    public RuleException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public RuleException(RuleResultCode RuleResultCode) {
        super(RuleResultCode.getMessage());
        this.code = RuleResultCode.getCode();
    }
    
    public RuleException(String message) {
        super(message);
        this.code = RuleResultCode.BUSINESS_ERROR.getCode();
    }
    
    public RuleException(RuleResultCode RuleResultCode, String message) {
        super(message);
        this.code = RuleResultCode.getCode();
    }
}