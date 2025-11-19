package com.wb.rules.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.wb.rules.event.RuleUpdateEvent;
import com.wb.rules.service.MessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class RuleUpdateConsumer {
    private final StringRedisTemplate stringRedisTemplate;
    private final RuleEngineService ruleEngineService;
    private final MessageLogService messageLogService;
    private final ObjectMapper objectMapper;
    @Value("${app.mq.max-retry-count:3}")
    private int maxRetryCount;
    private static final String MESSAGE_CACHE_KEY = "rule_update_processed";

    @RabbitListener(queues = "rule.update.queue")
    public void handleRuleUpdate(Message message, Channel channel,  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String msgId = message.getMessageProperties().getCorrelationId();
        if (msgId == null) {
            log.error("消息ID为空，拒绝消息");
            basicNack(channel, deliveryTag, false);
            return;
        }

        try {
            //幂等检查
            if (isMessageprocessed(msgId)){
                log.info("消息 {} 已处理，直接确认", msgId);
                basicAck(channel, deliveryTag);
                return;
            }

            // 2. 解析消息内容
            RuleUpdateEvent ruleUpdateEvent = parseMessage(message);
            if (ruleUpdateEvent == null) {
                throw new IllegalArgumentException("消息解析失败");
            }

            String ruleVersion = ruleUpdateEvent.getRuleVersion();
            String tenantId = ruleUpdateEvent.getTenantId() != null ? ruleUpdateEvent.getTenantId() : "default";
            log.info("开始处理规则更新消息: {}, 规则版本: {}, 租户: {}", msgId, ruleVersion, tenantId);

            // 3. 处理业务逻辑
            boolean success = processRuleUpdate(ruleUpdateEvent, ruleVersion, tenantId);
            if (success) {
                // 4. 处理成功，记录幂等性
                markMessageAsProcessed(msgId);
                basicAck(channel, deliveryTag);
                logger.info("规则更新消息处理成功: {}", msgId);
            } else {
                throw new RuntimeException("规则处理失败");
            }
        }catch (Exception e) {
            log.error("处理规则更新消息失败: {}", msgId, e);
            handleProcessingFailure(msgId, deliveryTag, channel, e);
        }
    }

    private boolean isMessageProcessed(String msgId) {
        Boolean exists = redisTemplate.opsForHash().hasKey(MESSAGE_CACHE_KEY, msgId);
        return Boolean.TRUE.equals(exists);
    }

    private RuleUpdateEvent parseMessage(Message message) {
        try {
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            return objectMapper.readValue(messageBody, RuleUpdateEvent.class);
        } catch (Exception e) {
            log.error("消息解析失败", e);
            return null;
        }
    }

    private boolean processRuleUpdate(RuleUpdateEvent event, String ruleVersion, String tenantId) {
        try {
            // 优先使用消息中的规则内容，如果没有则从Redis获取
            String ruleContent = event.getRuleContent();
            if (ruleContent == null) {
                ruleContent = ruleEngineService.getRuleContent(ruleVersion, tenantId);
            }

            if (ruleContent == null) {
                log.error("未找到规则内容，版本: {}, 租户: {}", ruleVersion, tenantId);
                return false;
            }

            // 动态加载规则到Drools引擎
            return ruleEngineService.loadRule(ruleContent, ruleVersion, tenantId);

        } catch (Exception e) {
            log.error("规则处理异常", e);
            return false;
        }
    }

    private void markMessageAsProcessed(String msgId) {
        try {
            redisTemplate.opsForHash().put(MESSAGE_CACHE_KEY, msgId, "processed");
            redisTemplate.expire(MESSAGE_CACHE_KEY, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("记录幂等性失败: {}", msgId, e);
        }
    }

    private void handleProcessingFailure(String msgId, Long deliveryTag,
                                         org.springframework.amqp.rabbit.listener.api.Channel channel, Exception e) {
        try {
            MessageLog messageLog = messageLogService.getMessageById(msgId);
            int currentRetryCount = messageLog != null ? messageLog.getCount() : 0;

            if (currentRetryCount >= maxRetryCount - 1) {
                logger.warn("消息 {} 已达到最大重试次数，转入死信队列", msgId);
                recordFailedMessage(msgId, e.getMessage());
                basicNack(channel, deliveryTag, false); // 不重新入队
            } else {
                logger.info("消息 {} 重新放回队列等待重试，当前重试次数: {}", msgId, currentRetryCount);
                basicNack(channel, deliveryTag, true); // 重新入队
            }
        } catch (Exception ex) {
            logger.error("处理失败逻辑异常", ex);
            basicNack(channel, deliveryTag, false);
        }
    }

    private void recordFailedMessage(String msgId, String errorMsg) {
        try {
            String failedKey = MESSAGE_CACHE_KEY + ":failed";
            redisTemplate.opsForHash().put(failedKey, msgId, errorMsg);
            redisTemplate.expire(failedKey, 72, TimeUnit.HOURS); // 失败消息保留3天
        } catch (Exception e) {
            logger.warn("记录失败消息失败: {}", msgId, e);
        }
    }

    private void basicAck(org.springframework.amqp.rabbit.listener.api.Channel channel, Long deliveryTag) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.basicAck(deliveryTag, false);
            }
        } catch (IOException e) {
            logger.error("消息确认失败", e);
        }
    }

    private void basicNack(org.springframework.amqp.rabbit.listener.api.Channel channel, Long deliveryTag, boolean requeue) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.basicNack(deliveryTag, false, requeue);
            }
        } catch (IOException e) {
            logger.error("消息拒绝失败", e);
        }
    }
}
