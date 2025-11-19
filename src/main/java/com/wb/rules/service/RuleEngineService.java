package com.wb.rules.service;

public interface RuleEngineService {
    /**
     * 动态加载规则
     */
    void loadRule(String ruleContent, String ruleVersion, String tenantId);

    /**
     * 执行规则
     */
    void executeRule(Object fact, String ruleVersion, String tenantId);

    /**
     * 获取规则内容
     */
    String getRuleContent(String ruleVersion, String tenantId);
}
