package com.wb.rules.service;

import com.wb.rules.entity.DroolsRules;
import com.wb.rules.repository.DroolsRulesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.internal.utils.KieHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleManagementService {
    private final DroolsRulesRepository droolsRulesRepository;
    private final DroolsDynamicService droolsDynamicService;

    /**
     * 创建新规则
     */
    public DroolsRules createRule(DroolsRules rule) {
        if (droolsRulesRepository.existsByRuleKey(rule.getRuleKey())) {
            throw new RuntimeException("规则键已存在: " + rule.getRuleKey());
        }

        //预编译验证规则语法
        validateRuleContent(rule.getRuleContent());
        DroolsRules savedRule = droolsRulesRepository.save(rule);
        droolsDynamicService.reloadRule(rule.getRuleKey());
        return savedRule;
    }

    /**
     * 更新规则
     */
    public DroolsRules updateRule(String ruleKey, DroolsRules ruleUpdate) {
        DroolsRules existingRule = droolsRulesRepository.findByRuleKeyAndStatusTrue(ruleKey)
                .orElseThrow(() -> new RuntimeException("规则不存在: " + ruleKey));

        // 验证新规则语法
        validateRuleContent(ruleUpdate.getRuleContent());

        existingRule.setRuleContent(ruleUpdate.getRuleContent());
        existingRule.setRuleName(ruleUpdate.getRuleName());
        existingRule.setDescription(ruleUpdate.getDescription());
        existingRule.setVersion(existingRule.getVersion() + 1);

        DroolsRules updatedRule = droolsRulesRepository.save(existingRule);
        // 重新加载规则
        droolsDynamicService.reloadRule(ruleKey);

        return updatedRule;
    }

    /**
     * 验证规则内容语法
     */
    private void validateRuleContent(String ruleContent) {
        try {
            KieHelper kieHelper = new KieHelper();
            kieHelper.addContent(ruleContent, ResourceType.DRL);
            Results results = kieHelper.verify();

            if (results.hasMessages(Message.Level.ERROR)) {
                throw new RuntimeException("规则语法错误: " + results.getMessages());
            }
        } catch (Exception e) {
            throw new RuntimeException("规则验证失败: " + e.getMessage());
        }
    }


}
