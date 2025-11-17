package com.wb.rules.controller;

import com.wb.rules.dto.RuleExecutionResult;
import com.wb.rules.entity.DroolsRules;
import com.wb.rules.entity.Order;
import com.wb.rules.repository.DroolsRulesRepository;
import com.wb.rules.service.DroolsDynamicService;
import com.wb.rules.service.RuleManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/rules")
@Slf4j
@RequiredArgsConstructor
public class RuleEngineController {

    private final DroolsDynamicService droolsDynamicService;
    private final RuleManagementService ruleManagementService;
    private final DroolsRulesRepository droolsRulesRepository;

    /**
     * 执行规则接口
     */
    @PostMapping("/execute/{ruleKey}")
    public ResponseEntity<RuleExecutionResult> executeRule(
            @PathVariable String ruleKey,
            @RequestBody Order order) {

        try {
            Order result = droolsDynamicService.executeRule(ruleKey, order);
             return ResponseEntity.ok(RuleExecutionResult.success(result));
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
            droolsDynamicService.reloadRule(ruleKey);
            return ResponseEntity.ok("规则重载成功: " + ruleKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("规则重载失败: " + e.getMessage());
        }
    }

    /**
     * 创建规则接口
     */
    @PostMapping
    public ResponseEntity<DroolsRules> createRule(@RequestBody DroolsRules rule) {
        try {
            DroolsRules createdRule = ruleManagementService.createRule(rule);
            return ResponseEntity.ok(createdRule);
        } catch (Exception e) {
            throw new RuntimeException("规则创建失败: " + e.getMessage());
        }
    }

    /**
     * 获取已加载规则列表
     */
    @GetMapping("/loaded")
    public ResponseEntity<Set<String>> getLoadedRules() {
        return ResponseEntity.ok(droolsDynamicService.getLoadedRuleKeys());
    }

    /**
     * 获取所有规则定义
     */
    @GetMapping
    public ResponseEntity<List<DroolsRules>> getAllRules() {
        return ResponseEntity.ok(droolsRulesRepository.findAll());
    }

}
