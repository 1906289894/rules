package com.wb.rules.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.wb.rules.common.exceptions.RuleException;
import com.wb.rules.entity.RuleDefinition;
import com.wb.rules.repository.RuleDefinitionRepository;
import com.wb.rules.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieBase;
import org.kie.api.builder.*;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEngineServiceImpl implements RuleEngineService {
    private final StringRedisTemplate redisTemplate;
    private final Cache<String, KieBase> kieBaseCache;
    private final RuleDefinitionRepository ruleDefinitionRepository;
    // 为每个规则提供编译锁，防止并发编译同一规则集
    private final ConcurrentHashMap<String, ReentrantLock> compileLocks = new ConcurrentHashMap<>();

    @Override
    public void loadRule(String ruleVersion, String ruleKey) {
        getOrCompileBase(ruleVersion, ruleKey);
    }

    /**
     * 获取或编译KieBase
     */
    private KieBase getOrCompileBase(String ruleVersion, String ruleKey) {
        String cacheKey = buildCacheKey(ruleKey, ruleVersion);
        KieBase kieBase = kieBaseCache.getIfPresent(cacheKey);
        if (kieBase != null) {
            return kieBase;
        }

        // 缓存未命中，需要编译。为防止并发编译，对 ruleId 加锁
        ReentrantLock compileLock = compileLocks.computeIfAbsent(cacheKey, k -> new ReentrantLock());
        compileLock.lock();
        try {
            // 获取锁后再次检查缓存（Double-Check），防止其他线程已编译完成
            kieBase = kieBaseCache.getIfPresent(cacheKey);
            if (kieBase != null) {
                return kieBase;
            }

            // 从Redis或数据库获取规则内容
            RuleDefinition ruleDefinition = ruleDefinitionRepository.findByRuleKeyAndVersion(ruleKey, ruleVersion).orElseThrow(() -> new RuleException("未找到规则"));
            String ruleContent = ruleDefinition.getRuleContent();
            if (ruleContent == null) {
                throw new RuntimeException("未找到规则内容");
            }

            // 编译规则
            kieBase = compileRule(ruleContent);
            if (kieBase != null) {
                // 编译成功，放入缓存
                kieBaseCache.put(cacheKey, kieBase);
                log.info("规则编译并缓存成功: {}", cacheKey);
            }
            return kieBase;
        } finally {
            compileLock.unlock();
        }
    }

    /**
     * 编译DRL规则内容
     */
    private KieBase compileRule(String drlContent) {
        KieHelper kieHelper = new KieHelper();
        try {
            kieHelper.addContent(drlContent, ResourceType.DRL);
            Results results = kieHelper.verify();

            if (results.hasMessages(Message.Level.ERROR)) {
                String errors = results.getMessages().toString();
                log.error("规则编译错误: {}", errors);
                throw new RuleException("规则编译失败: " + errors);
            }
            return kieHelper.build();
        }catch (Exception e){
            log.error("规则编译失败", e);
            throw new RuleException("规则编译失败", e);
        }
    }

    @Override
    public void executeRule(Object fact, String ruleVersion, String ruleKey) {
        KieBase kieBase = getOrCompileBase(ruleVersion, ruleKey);
        KieSession kieSession = kieBase.newKieSession();
        try {
            kieSession.insert(fact);
            int firedRules = kieSession.fireAllRules();
            log.debug("规则执行完成，触发规则数量：{}", firedRules);
        }catch (Exception e){
            log.error("规则执行失败", e);
            throw new RuntimeException("规则执行失败", e);
        }finally {
            kieSession.dispose();
        }
    }

    @Override
    public Set<String> getLoadedRuleKeys() {
        return kieBaseCache.asMap().keySet();
    }

    private String buildCacheKey(String ruleKey, String version) {
        return ruleKey + ":" + version;
    }
}
