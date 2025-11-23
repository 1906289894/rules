package com.wb.rules.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
public class RuleUpdateEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private String ruleVersion; //规则版本
    private String ruleContent; //规则实体
    private String ruleType; // DRL, Excel等
    private String ruleKey; //规则编号
}