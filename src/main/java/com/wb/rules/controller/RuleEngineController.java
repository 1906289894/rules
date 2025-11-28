package com.wb.rules.controller;

import com.wb.rules.common.result.R;
import com.wb.rules.dto.RuleExecutionResult;
import com.wb.rules.entity.RuleDefinition;
import com.wb.rules.entity.Order;
import com.wb.rules.repository.RuleDefinitionRepository;
import com.wb.rules.service.RuleEngineService;
import com.wb.rules.service.impl.RuleServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/rule")
@Slf4j
@RequiredArgsConstructor
public class RuleEngineController {

    private final RuleEngineService ruleEngineService;
    private final RuleServiceImpl ruleServiceImpl;
    private final RuleDefinitionRepository ruleDefinitionRepository;

    /**
     * 执行规则接口
     */
    @PostMapping("/execute/{ruleKey}")
    public ResponseEntity<RuleExecutionResult> executeRule(
            @PathVariable String ruleKey,
            @RequestBody Order order) {

        try {
             ruleEngineService.executeRule(order, ruleKey, "");
             return ResponseEntity.ok(RuleExecutionResult.success(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(RuleExecutionResult.error(e.getMessage()));
        }
    }

    /**
     * 重载规则接口
     */
    @PostMapping("/reload/{ruleKey}")
    public ResponseEntity<String> reloadRule(@PathVariable String ruleKey) {
        try {
            //TODO
            return ResponseEntity.ok("规则重载成功: " + ruleKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("规则重载失败: " + e.getMessage());
        }
    }

    /**
     * 创建规则接口
     */
    @PostMapping("/add")
    public R<RuleDefinition> createRule(@RequestBody RuleDefinition rule) {
        RuleDefinition createdRule = ruleServiceImpl.createRule(rule);
        return R.success(createdRule);
    }

    /**
     * 获取已加载规则列表
     */
    @GetMapping("/loaded")
    public ResponseEntity<Set<String>> getLoadedRules() {
        return ResponseEntity.ok(ruleEngineService.getLoadedRuleKeys());
    }

    /**
     * 获取所有规则定义
     */
    @GetMapping
    public ResponseEntity<List<RuleDefinition>> getAllRules() {
        return ResponseEntity.ok(ruleDefinitionRepository.findAll());
    }
}
