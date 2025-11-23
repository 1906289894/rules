package com.wb.rules.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.wb.rules.event.RuleUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DeadLetterConsumer {

    @RabbitListener(queues = "rule.dlq")
    public void handleDeadLetterMessage(Message message, Channel channel){

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
        String messageId = message.getMessageProperties().getMessageId();

        try {
            log.warn("收到死信消息: ID={}, 消息体: {}", messageId, messageBody);

            // 获取死信原因（x-death 头部信息）
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> xDeath = (List<Map<String, Object>>) headers.get("x-death");

            if (xDeath != null && !xDeath.isEmpty()) {
                Map<String, Object> deathInfo = xDeath.get(0);
                log.warn("死信消息详情 - 原因: {}, 交换机: {}, 路由键: {}, 时间: {}",
                        deathInfo.get("reason"),
                        deathInfo.get("exchange"),
                        deathInfo.get("routing-keys"),
                        deathInfo.get("time"));
            }

            // 处理死信消息
            boolean processSuccess = processDeadLetterMessage(messageBody, messageId, xDeath);

            if (processSuccess) {
                // 处理成功，确认消息
                channel.basicAck(deliveryTag, false);
                log.info("死信消息处理完成: {}", messageId);
            } else {
                // 处理失败，根据策略决定是否重新投递
                handleDeadLetterProcessFailure(channel, deliveryTag, messageId);
            }

        } catch (Exception e) {
            log.error("处理死信消息异常: {}", messageId, e);
            handleDeadLetterException(channel, deliveryTag, messageId, e);
        }
    }

    /**
     * 处理死信消息业务逻辑
     */
    private boolean processDeadLetterMessage(String messageBody, String messageId,
                                             List<Map<String, Object>> xDeath) {
        try {
            // 1. 解析消息内容
            RuleUpdateEvent event = parseMessageBody(messageBody);
            if (event == null) {
                log.error("死信消息解析失败: {}", messageId);
                return false;
            }

            // 2. 记录死信信息（数据库、文件、日志等）
            recordDeadLetterInfo(messageId, event, xDeath);

            // 3. 发送告警通知
            sendDeadLetterAlert(messageId, event, xDeath);

            // 4. 尝试自动修复或记录需要人工干预
            return tryAutoRecovery(messageId, event);

        } catch (Exception e) {
            log.error("死信消息业务处理异常: {}", messageId, e);
            return false;
        }
    }

    /**
     * 处理死信消息处理失败的情况
     */
    private void handleDeadLetterProcessFailure(Channel channel, long deliveryTag, String messageId) {
        try {
            // 策略1：直接确认，避免无限循环（推荐）
            channel.basicAck(deliveryTag, false);
            log.warn("死信消息处理失败，已确认消息: {}", messageId);

            // 策略2：如果需要重试，可以延迟后重新投递
            // channel.basicNack(deliveryTag, false, true);
            // Thread.sleep(30000); // 延迟30秒

        } catch (Exception e) {
            log.error("处理死信消息失败逻辑异常: {}", messageId, e);
        }
    }

    /**
     * 处理死信消息异常
     */
    private void handleDeadLetterException(Channel channel, long deliveryTag, String messageId, Exception e) {
        try {
            // 发生异常时，直接确认消息，避免死信队列堆积
            channel.basicAck(deliveryTag, false);
            log.warn("死信消息处理异常，已确认消息: {}", messageId);

            // 记录异常信息，方便后续排查
            recordExceptionLog(messageId, e);

        } catch (Exception ex) {
            log.error("确认死信消息异常: {}", messageId, ex);
        }
    }

    private RuleUpdateEvent parseMessageBody(String messageBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(messageBody, RuleUpdateEvent.class);
        } catch (Exception e) {
            log.error("解析消息体失败: {}", messageBody, e);
            return null;
        }
    }

    private void recordDeadLetterInfo(String messageId, RuleUpdateEvent event,
                                      List<Map<String, Object>> xDeath) {
        // 记录死信信息到数据库或文件
        log.error("死信记录 - 消息ID: {}, 规则Key: {}, 版本: {}, 死信原因: {}",
                messageId, event.getRuleKey(), event.getRuleVersion(),
                xDeath != null ? xDeath.toString() : "unknown");
    }

    private void sendDeadLetterAlert(String messageId, RuleUpdateEvent event,
                                     List<Map<String, Object>> xDeath) {
        // 发送告警通知（邮件、钉钉、短信等）
        String alertMessage = String.format(
                "🚨 规则更新死信告警\n消息ID: %s\n规则: %s\n版本: %s\n时间: %s\n原因: %s",
                messageId, event.getRuleKey(), event.getRuleVersion(),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                xDeath != null ? xDeath.toString() : "未知"
        );

        log.error("死信告警: {}", alertMessage);
    }

    private boolean tryAutoRecovery(String messageId, RuleUpdateEvent event) {
        // 尝试自动恢复逻辑
        // 例如：检查规则语法错误，尝试修复后重新投递
        log.info("尝试自动恢复死信消息: {}", messageId);
        return false; // 默认需要人工干预
    }

    private void recordExceptionLog(String messageId, Exception e) {
        // 记录异常日志到文件或数据库
        log.error("死信处理异常记录 - 消息ID: {}, 异常: {}", messageId, e.getMessage());
    }
}