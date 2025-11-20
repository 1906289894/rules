package com.wb.rules.service.impl;

import com.wb.rules.common.exceptions.RuleException;
import com.wb.rules.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEngineServiceImpl implements RuleEngineService {
    private final StringRedisTemplate redisTemplate;

    //使用ConcurrentHashMap存储不同版本的KieSession
    private final Map<String, KieSession> kieSessionMap = new ConcurrentHashMap<>();
    private final Map<String, KieContainer> kieContainerMap = new ConcurrentHashMap<>();

    @Override
    public void loadRule(String ruleContent, String ruleVersion, String ruleKey) {
        String sessionKey = buildSessionKey(ruleKey, ruleVersion);
        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kfs = kieServices.newKieFileSystem();
            //todo 从数据库重新加载

            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
            Results results = kieBuilder.getResults();

            if (results.hasMessages(Message.Level.ERROR)) {
                log.error("规则编译错误：{}", results.getMessages());
                throw new RuleException("规则加载失败");
            }

            //释放旧的KieSession
            disposeOldSession(sessionKey);

            // 创建新的KieContainer和KieSession
            KieRepository repository = kieServices.getRepository();
            repository.addKieModule(repository::getDefaultReleaseId);
            KieContainer kieContainer = kieServices.newKieContainer(repository.getDefaultReleaseId());
            KieSession kieSession = kieContainer.newKieSession();

            //缓存新的KieSession和KieContainer
            kieSessionMap.put(sessionKey, kieSession);
            kieContainerMap.put(sessionKey, kieContainer);

            //将规则内容缓存到Redis
            cacheRuleToRedis(ruleContent, ruleVersion, ruleKey);
            log.info("规则引擎更新成功，版本: {}, 租户: {}", ruleVersion, ruleKey);
        }catch (Exception e){
            log.error("规则引擎更新失败，版本: {}, 租户: {}", ruleVersion, ruleKey, e);
            throw new RuleException("规则加载失败");
        }
    }

    private void cacheRuleToRedis(String ruleContent, String ruleVersion, String ruleKey) {
        String redisKey = "drools_rules:" + ruleKey;
        redisTemplate.opsForHash().put(redisKey, ruleVersion, ruleContent);
        //设置过期时间24小时
        redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
    }

    private void disposeOldSession(String sessionKey) {
        KieSession oldKieSession = kieSessionMap.remove(sessionKey);
        KieContainer oldKieContainer = kieContainerMap.remove(sessionKey);
        if (oldKieContainer != null) {
            try {
                oldKieContainer.dispose();
            }catch (Exception e){
                log.warn("释放旧的KieContainer失败", e);
            }
        }
        if (oldKieSession != null) {
            try {
                oldKieSession.dispose();
            }catch (Exception e){
                log.warn("释放旧的oldKieSession失败", e);
            }
        }
    }

    private String buildSessionKey(String ruleKey, String ruleVersion) {
        return ruleKey + ":" + ruleVersion;
    }

    @Override
    public void executeRule(Object fact, String ruleVersion, String ruleKey) {
        String sessionKey  = buildSessionKey(ruleKey, ruleVersion);
        KieSession kieSession = kieSessionMap.get(sessionKey);

        if (Objects.nonNull(kieSession)) {
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
        } else {
            log.warn("未找到对应的规则会话，版本: {}, 租户: {}", ruleVersion, ruleKey);
        }
    }

    @Override
    public String getRuleContent(String ruleVersion, String ruleKey) {
        String redisKey = "drools_rules:" + ruleKey;
        return (String) redisTemplate.opsForHash().get(redisKey, ruleVersion);
    }
}
