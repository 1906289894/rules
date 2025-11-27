package com.wb.rules.service;

import com.wb.rules.entity.RuleDefinition;

public interface RuleService {

    /**
     * 创建规则
     * @param rule 规则实体
     * @return 规则实体
     */
    RuleDefinition createRule(RuleDefinition rule);

    /**
     * 更新规则
     * @param ruleUpdate 规则实体
     * @return 规则实体
     */
    RuleDefinition updateRule(String ruleKey, RuleDefinition ruleUpdate);
}
