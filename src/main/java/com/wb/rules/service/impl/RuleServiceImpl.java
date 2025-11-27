package com.wb.rules.service.impl;

import com.wb.rules.entity.RuleDefinition;
import com.wb.rules.event.RuleUpdateEvent;
import com.wb.rules.mq.RuleUpdateProducer;
import com.wb.rules.repository.RuleDefinitionRepository;
import com.wb.rules.service.RuleService;
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
public class RuleServiceImpl implements RuleService {
    private final RuleDefinitionRepository ruleDefinitionRepository;
    private final RuleUpdateProducer ruleUpdateProducer;

    /**
     * 创建新规则
     */
    public RuleDefinition createRule(RuleDefinition rule) {
        if (ruleDefinitionRepository.existsByRuleKey(rule.getRuleKey())) {
            throw new RuntimeException("规则键已存在: " + rule.getRuleKey());
        }
        //预编译验证规则语法
        validateRuleContent(rule.getRuleContent());
        RuleDefinition savedRule = ruleDefinitionRepository.save(rule);
        RuleUpdateEvent event = RuleUpdateEvent.builder()
                .ruleType("DRL")
                .ruleKey(savedRule.getRuleKey())
                .ruleVersion(savedRule.getVersion())
                .build();
        ruleUpdateProducer.sendRuleUpdateMessage(event);
        return savedRule;
    }

    /**
     * 更新规则
     */
    public RuleDefinition updateRule(String ruleKey, RuleDefinition ruleUpdate) {
        RuleDefinition existingRule = ruleDefinitionRepository.findByRuleKeyAndStatusTrue(ruleKey)
                .orElseThrow(() -> new RuntimeException("规则不存在: " + ruleKey));

        // 验证新规则语法
        validateRuleContent(ruleUpdate.getRuleContent());

        existingRule.setRuleContent(ruleUpdate.getRuleContent());
        existingRule.setRuleName(ruleUpdate.getRuleName());
        existingRule.setDescription(ruleUpdate.getDescription());
        existingRule.setVersion(existingRule.getVersion() + 1);

        RuleDefinition updatedRule = ruleDefinitionRepository.save(existingRule);
        RuleUpdateEvent event = RuleUpdateEvent.builder()
                .ruleType("DRL")
                .ruleKey(updatedRule.getRuleKey())
                .ruleVersion(updatedRule.getVersion())
                .build();
        ruleUpdateProducer.sendRuleUpdateMessage(event);
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
