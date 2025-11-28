package com.wb.rules.service.impl;

import com.wb.rules.common.exceptions.RuleException;
import com.wb.rules.entity.Order;
import com.wb.rules.entity.RuleDefinition;
import com.wb.rules.repository.RuleDefinitionRepository;
import com.wb.rules.service.OrderService;
import com.wb.rules.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final RuleEngineService ruleEngineService;
    private final RuleDefinitionRepository ruleDefinitionRepository;
    @Override
    public Order test(Order order) {
        RuleDefinition ruleDefinition = ruleDefinitionRepository.findTopByRuleKeyOrderByVersionDesc("order").orElseThrow(() -> new RuleException("规则不存在"));
        ruleEngineService.executeRule(order, ruleDefinition.getRuleKey(), ruleDefinition.getVersion());
        return order;
    }
}
