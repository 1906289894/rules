package com.wb.rules.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wb.rules.common.exceptions.RuleException;
import com.wb.rules.entity.MessageLog;
import com.wb.rules.event.RuleUpdateEvent;
import com.wb.rules.service.MessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleUpdateProducer {

    private final RabbitTemplate rabbitTemplate;
    private final MessageLogService messageLogService;
    private final ObjectMapper objectMapper;

    /**
     * 发送规则更新消息 - 同步方式
     */
    @Transactional
    public String sendRuleUpdateMessage(RuleUpdateEvent ruleUpdateEvent) {
        String msgId = UUID.randomUUID().toString();

        try {
            // 验证消息参数
            validateRuleUpdateEvent(ruleUpdateEvent);

            // 保存消息记录（状态为待发送）
            MessageLog messageLog = createMessageLog(msgId, ruleUpdateEvent, 0); // 0-待发送
            messageLogService.insert(messageLog);

            // 发送消息
            rabbitTemplate.convertAndSend(
                    "rule.exchange",
                    "rule.update",
                    ruleUpdateEvent,
                    message -> {
                        // 设置消息ID，用于幂等性检查
                        message.getMessageProperties().setCorrelationId(msgId);
                        message.getMessageProperties().setMessageId(msgId);
                        return message;
                    },
                    new CorrelationData(msgId)
            );

            // 4. 更新消息状态为已发送（这里依赖confirm回调来更新状态）
            log.info("规则更新消息发送成功，消息ID: {}", msgId);
            return msgId;

        } catch (Exception e) {
            // 发送失败，更新状态
            handleSendFailure(msgId, ruleUpdateEvent, e);
            throw new RuleException("规则更新消息发送失败");
        }
    }

    /**
     * 发送规则更新消息 - 异步方式
     */
    public CompletableFuture<String> sendRuleUpdateMessageAsync(RuleUpdateEvent ruleUpdateEvent) {
        return CompletableFuture.supplyAsync(() -> sendRuleUpdateMessage(ruleUpdateEvent));
    }

    /**
     * 带重试的消息发送
     */
    public String sendRuleUpdateMessageWithRetry(RuleUpdateEvent ruleUpdateEvent, int maxRetries) {
        String msgId = UUID.randomUUID().toString();
        int retryCount = 0;

        while (retryCount <= maxRetries) {
            try {
                if (retryCount > 0) {
                    log.info("第{}次重试发送消息: {}", retryCount, msgId);
                }

                // 保存或更新消息记录
                MessageLog messageLog = createMessageLog(msgId, ruleUpdateEvent, retryCount);
                if (retryCount == 0) {
                    messageLogService.insert(messageLog);
                } else {
                    messageLogService.updateRetryInfo(msgId, retryCount, "重试发送");
                }

                // 发送消息
                rabbitTemplate.convertAndSend("rule.exchange", "rule.update", ruleUpdateEvent,
                        new CorrelationData(msgId));

                log.info("规则更新消息发送成功，消息ID: {}", msgId);
                return msgId;

            } catch (Exception e) {
                retryCount++;
                log.warn("消息发送失败，准备重试。消息ID: {}, 重试次数: {}", msgId, retryCount, e);

                if (retryCount > maxRetries) {
                    // 重试次数用尽，标记为最终失败
                    messageLogService.markFinalFailure(msgId, "发送重试次数用尽: " + e.getMessage());
                    throw new RuleException("消息发送失败，重试次数用尽");
                }

                // 指数退避等待
                try {
                    Thread.sleep(calculateRetryDelay(retryCount));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuleException("消息发送重试被中断");
                }
            }
        }

        return msgId;
    }

    /**
     * 验证消息参数
     */
    private void validateRuleUpdateEvent(RuleUpdateEvent event) {
        if (event == null) {
            throw new RuleException("规则更新事件不能为空");
        }
        if (event.getRuleVersion() == null || event.getRuleVersion().trim().isEmpty()) {
            throw new RuleException("规则版本不能为空");
        }
        if (event.getRuleContent() == null || event.getRuleContent().trim().isEmpty()) {
            log.warn("规则内容为空，消费者将尝试从存储加载");
        }
    }

    /**
     * 创建消息记录
     */
    private MessageLog createMessageLog(String msgId, RuleUpdateEvent event, int retryCount) {
        return MessageLog.builder()
                .msgId(msgId)
                .ruleVersion(event.getRuleVersion())
                .ruleKey(event.getRuleKey())
                .status(0) // 0-待发送/发送中
                .count(retryCount)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .tryTime(LocalDateTime.now().plusMinutes(1)) // 正确的1分钟后重试
                .build();
    }

    /**
     * 处理发送失败
     */
    private void handleSendFailure(String msgId, RuleUpdateEvent event, Exception e) {
        try {
            // 更新消息状态为发送失败
            messageLogService.markSendFailure(msgId, e.getMessage());
            log.error("规则更新消息发送失败，消息ID: {}", msgId, e);
        } catch (Exception ex) {
            log.error("更新消息发送状态失败，消息ID: {}", msgId, ex);
            // 这里可以添加告警通知
        }
    }

    /**
     * 序列化消息内容（用于消息追踪）
     */
    private String serializeMessageContent(RuleUpdateEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("序列化消息内容失败", e);
            return "序列化失败";
        }
    }

    /**
     * 计算重试延迟（指数退避）
     */
    private long calculateRetryDelay(int retryCount) {
        return (long) (Math.pow(2, retryCount) * 1000); // 1s, 2s, 4s, 8s...
    }

    /**
     * 查询消息发送状态
     */
    public MessageLog getMessageStatus(String msgId) {
        return messageLogService.getMessageById(msgId);
    }

    /**
     * 重新发送失败的消息
     */
    public boolean resendMessage(String msgId) {
        try {
            MessageLog messageLog = messageLogService.getMessageById(msgId);
            if (messageLog == null) {
                log.error("消息记录不存在: {}", msgId);
                return false;
            }

            RuleUpdateEvent event = deserializeMessageContent(messageLog.getMessageContent());
            if (event == null) {
                log.error("反序列化消息内容失败: {}", msgId);
                return false;
            }

            String newMsgId = sendRuleUpdateMessageWithRetry(event, 1);
            log.info("消息重新发送成功，原消息ID: {}, 新消息ID: {}", msgId, newMsgId);
            return true;

        } catch (Exception e) {
            log.error("重新发送消息失败: {}", msgId, e);
            return false;
        }
    }

    /**
     * 反序列化消息内容
     */
    private RuleUpdateEvent deserializeMessageContent(String content) {
        try {
            return objectMapper.readValue(content, RuleUpdateEvent.class);
        } catch (Exception e) {
            log.error("反序列化消息内容失败", e);
            return null;
        }
    }
}