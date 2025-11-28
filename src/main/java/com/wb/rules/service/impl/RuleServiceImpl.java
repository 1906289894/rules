package com.wb.rules.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.wb.rules.common.exceptions.RuleException;
import com.wb.rules.entity.RuleDefinition;
import com.wb.rules.event.RuleUpdateEvent;
import com.wb.rules.mq.RuleUpdateProducer;
import com.wb.rules.repository.RuleDefinitionRepository;
import com.wb.rules.service.RuleService;
import com.wb.rules.utils.VersionUtil;
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
            throw new RuleException("规则键已存在: " + rule.getRuleKey());
        }
        //预编译验证规则语法
        validateRuleContent(rule.getRuleContent());
        RuleDefinition savedRule = ruleDefinitionRepository.save(rule);
        RuleUpdateEvent event = RuleUpdateEvent.builder()
                .ruleKey(savedRule.getRuleKey())
                .ruleVersion(VersionUtil.getVersion())
                .build();
        ruleUpdateProducer.sendRuleUpdateMessage(event);
        return savedRule;
    }

    /**
     * 更新规则
     */
    public RuleDefinition updateRule(RuleDefinition ruleUpdate) {
        String ruleKey = ruleUpdate.getRuleKey();
        String currentVersion = ruleUpdate.getVersion();
        RuleDefinition existingRule = ruleDefinitionRepository.findByRuleKeyAndVersion(ruleKey, currentVersion)
                .orElseThrow(() -> new RuntimeException("规则不存在: " + ruleKey));

        // 验证新规则语法
        validateRuleContent(ruleUpdate.getRuleContent());

        RuleDefinition newRule = BeanUtil.copyProperties(existingRule, RuleDefinition.class);
        newRule.setId(null);
        newRule.setVersion(VersionUtil.updateVersion(currentVersion));

        ruleDefinitionRepository.save(newRule);
        RuleUpdateEvent event = RuleUpdateEvent.builder()
                .ruleKey(newRule.getRuleKey())
                .ruleVersion(newRule.getVersion())
                .build();
        ruleUpdateProducer.sendRuleUpdateMessage(event);
        return newRule;
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
                throw new RuleException("规则语法错误: " + results.getMessages());
            }
        } catch (Exception e) {
            throw new RuleException("规则验证失败: " + e.getMessage());
        }
    }


}
