package com.wb.rules.task;

import com.wb.rules.entity.MessageLog;
import com.wb.rules.event.RuleUpdateEvent;
import com.wb.rules.service.MessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageRetryTask {
    private final RabbitTemplate rabbitTemplate;
    private final MessageLogService messageLogService;

    @Scheduled(cron = "0/30 * * * * ?") // 每30秒执行一次
    public void messageRetry() {
        // 查询需要重试的消息
        List<MessageLog> needRetryMessages = messageLogService.getNeedRetryMessages();
        for (MessageLog message : needRetryMessages) {
            if (message.getCount() >= 3) {
                //超过最大尝试次数，标记失败
                messageLogService.updateStatus(message.getMsgId(), 2);
                log.warn("消息 {} 重试次数已超过3次，标记为失败", message.getMsgId());
            } else {
                // 执行重试
                log.info("消息 {} 开始第 {} 次重试", message.getMsgId(), message.getCount() + 1);
                RuleUpdateEvent event = new RuleUpdateEvent();
                event.setRuleVersion(message.getRuleVersion());
                messageLogService.updateCount(message.getMsgId(), LocalDateTime.now());
                rabbitTemplate.convertAndSend("rule.exchange","rule.update", event, new CorrelationData(message.getMsgId()));
            }
        }
    }
}
