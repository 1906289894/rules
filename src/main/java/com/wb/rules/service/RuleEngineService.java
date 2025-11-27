package com.wb.rules.service;

import java.util.Set;

public interface RuleEngineService {
    /**
     * 动态加载规则
     */
    void loadRule(String ruleVersion, String ruleKey);

    /**
     * 执行规则
     */
    void executeRule(Object fact, String ruleVersion, String ruleKey);

    /**
     * 获取所有已加载的规则键
     */
    Set<String> getLoadedRuleKeys();
}
