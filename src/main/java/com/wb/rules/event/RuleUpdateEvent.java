package com.wb.rules.event;

import jakarta.persistence.Column;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
public class RuleUpdateEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private String ruleVersion;
    private String ruleContent;
    private String ruleType; // DRL, Excel等
    private String ruleKey;
}