package com.wb.rules.service;

import com.wb.rules.entity.DroolsRules;
import com.wb.rules.entity.Order;
import com.wb.rules.repository.DroolsRulesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class DroolsDynamicService {

    private final DroolsRulesRepository rulesRepository;
    //规则缓存
    private final ConcurrentHashMap<String, KieBase> rulesCache = new ConcurrentHashMap<>();

    //根据规则键执行规则
    public Order executeRule(String ruleKey, Order order){
        KieBase kieBase = getOrLoadKieBase(ruleKey);
        KieSession kieSession = kieBase.newKieSession();

        try {
            kieSession.insert(order);
            int firedRules = kieSession.fireAllRules();
            log.info("规则执行完成：规则键={}, 触发数={}", ruleKey, firedRules);
        }finally {
            kieSession.dispose();
        }
        return order;
    }

    //获取或者加载KieBase
    private KieBase getOrLoadKieBase(String ruleKey) {
        return rulesCache.computeIfAbsent(ruleKey, key ->{
            DroolsRules rules =rulesRepository.findByRuleKeyAndStatusTrue(key).orElseThrow(() -> new RuntimeException("规则不存在或已禁用" + key));
            return compileRule(rules.getRuleContent());
        });
    }

    /**
     * 编译规则内容
     */
    private KieBase compileRule(String ruleContent) {
        try {
            KieHelper kieHelper = new KieHelper();
            kieHelper.addContent(ruleContent, ResourceType.DRL);

            Results results = kieHelper.verify();
            if (results.hasMessages(Message.Level.ERROR)) {
                String errorMsg = results.getMessages().toString();
                log.error("规则编译错误：{}", errorMsg);
                throw new RuntimeException("规则语法错误: " + errorMsg);
            }
            return kieHelper.build();
        } catch (Exception e) {
            log.error("规则编译失败",e);
            throw new RuntimeException("规则编译失败：" + e.getMessage());
        }
    }

    /**
     * 重新加载规则（热更新）
     */
    public void reloadRule(String ruleKey){
        try {
            rulesCache.remove(ruleKey);
            getOrLoadKieBase(ruleKey);
            log.info("规则重载成功");
        }catch (Exception e){
            log.error("重载规则失败，规则键={}, {}",ruleKey, e.getMessage());
        }
    }

    /**
     * 获取所有已加载的规则键
     */
    public Set<String> getLoadedRuleKeys(){
        return rulesCache.keySet();
    }
}
