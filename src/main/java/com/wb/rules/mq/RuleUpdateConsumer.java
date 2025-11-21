package com.wb.rules.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wb.rules.common.exceptions.RuleException;
import com.wb.rules.event.RuleUpdateEvent;
import com.wb.rules.service.MessageLogService;
import com.wb.rules.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class RuleUpdateConsumer {

    private final RuleEngineService ruleEngineService;
    private final MessageLogService messageLogService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String MESSAGE_CACHE_KEY = "rule_update_processed";

    /**
     * 规则更新消息消费者
     */
    @RabbitListener(queues = "rule.update.queue", containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void handleRuleUpdate(org.springframework.amqp.core.Message message) {
        String msgId = getMessageId(message);
        if (msgId == null) {
            log.error("消息ID为空，丢弃消息");
            throw new RuleException("消息ID不能为空");
        }

        try {
            // 幂等性检查 - 在业务开始前检查
            if (isMessageProcessed(msgId)) {
                log.info("消息已处理，直接跳过: {}", msgId);
                return;
            }

            // 解析消息
            RuleUpdateEvent ruleUpdateEvent = parseMessage(message);
            validateRuleUpdateEvent(ruleUpdateEvent);

            String ruleVersion = ruleUpdateEvent.getRuleVersion();
            String ruleKey = ruleUpdateEvent.getRuleKey() != null ? ruleUpdateEvent.getRuleKey() : "default";

            log.info("开始处理规则更新消息: {}, 版本: {}, 规则key: {}", msgId, ruleVersion, ruleKey);

            // 记录消息开始处理（在事务中）
            // todo messageLogService.recordProcessing(msgId, ruleVersion, tenantId);

            // 处理业务逻辑
            processRuleUpdate(ruleUpdateEvent, ruleVersion, ruleKey);

            // 标记处理成功（在事务中）
            // todo messageLogService.markSuccess(msgId);
            markMessageAsProcessed(msgId); // Redis幂等性标记

            log.info("规则更新消息处理成功: {}", msgId);

        } catch (IllegalArgumentException e) {
            // 参数错误，不重试，直接记录失败
            log.error("消息参数错误，丢弃消息: {}", msgId, e);
            messageLogService.recordFailure(msgId, e.getMessage());
            throw new AmqpRejectAndDontRequeueException("消息参数错误，不重试"); // 直接进入死信队列
        } catch (Exception e) {
            // 业务异常，进行重试
            log.error("处理规则更新消息失败，将进行重试: {}", msgId, e);
            messageLogService.recordRetry(msgId, e.getMessage());
            throw new RuleException("规则更新处理失败，需要重试");
        }
    }

    /**
     * 获取消息ID
     */
    private String getMessageId(org.springframework.amqp.core.Message message) {
        String msgId = message.getMessageProperties().getCorrelationId();
        if (msgId == null) {
            // 尝试从消息头获取
            msgId = (String) message.getMessageProperties().getHeaders().get("msgId");
        }
        return msgId;
    }

    /**
     * 幂等性检查 - 优先检查Redis，再检查数据库
     */
    private boolean isMessageProcessed(String msgId) {
        // 1. 检查Redis缓存
        Boolean exists = redisTemplate.opsForHash().hasKey(MESSAGE_CACHE_KEY, msgId);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }

        // 2. 检查数据库（防止Redis缓存失效）
        return messageLogService.isMessageProcessed(msgId);
    }

    /**
     * 解析消息
     */
    private RuleUpdateEvent parseMessage(org.springframework.amqp.core.Message message) {
        try {
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            return objectMapper.readValue(messageBody, RuleUpdateEvent.class);
        } catch (Exception e) {
            log.error("消息解析失败: {}", new String(message.getBody(), StandardCharsets.UTF_8), e);
            throw new RuleException("消息格式错误");
        }
    }

    /**
     * 验证消息参数
     */
    private void validateRuleUpdateEvent(RuleUpdateEvent event) {
        if (event == null) {
            throw new RuleException("消息内容不能为空");
        }
        if (event.getRuleVersion() == null || event.getRuleVersion().trim().isEmpty()) {
            throw new RuleException("规则版本不能为空");
        }
    }

    /**
     * 处理规则更新业务
     */
    private void processRuleUpdate(RuleUpdateEvent event, String ruleVersion, String ruleKey) {
        try {
            // 优先使用消息中的规则内容
            String ruleContent = event.getRuleContent();
            if (ruleContent == null) {
                // 从Redis获取规则内容
                ruleContent = ruleEngineService.getRuleContent(ruleVersion, ruleKey);
            }

            if (ruleContent == null) {
                throw new RuleException("未找到规则内容，版本: " + ruleVersion + " 规则key: " + ruleKey);
            }
            // 动态加载规则到Drools引擎
            ruleEngineService.loadRule(ruleContent, ruleVersion, ruleKey);
        } catch (Exception e) {
            log.error("规则处理异常: 版本={}, 规则key={}", ruleVersion, ruleKey, e);
            throw e; // 重新抛出，让重试机制处理
        }
    }

    /**
     * 标记消息已处理（Redis幂等性）
     */
    private void markMessageAsProcessed(String msgId) {
        try {
            redisTemplate.opsForHash().put(MESSAGE_CACHE_KEY, msgId, "processed");
            redisTemplate.expire(MESSAGE_CACHE_KEY, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("记录Redis幂等性失败: {}", msgId, e);
            // Redis操作失败不影响主流程，因为数据库事务已提交
        }
    }
}