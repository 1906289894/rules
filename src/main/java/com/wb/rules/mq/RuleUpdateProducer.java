package com.wb.rules.mq;

import com.wb.rules.entity.MessageLog;
import com.wb.rules.event.RuleUpdateEvent;
import com.wb.rules.service.MessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleUpdateProducer {
    private final RabbitTemplate rabbitTemplate;
    private final MessageLogService messageLogService;

    public void sendRuleUpdateMessage(RuleUpdateEvent ruleUpdateEvent) {
        String msgId = UUID.randomUUID().toString();
        // 保存消息到日志表
        MessageLog messageLog = new MessageLog();
        messageLog.setMsgId(msgId);
        messageLog.setRuleVersion(ruleUpdateEvent.getRuleVersion());
        messageLog.setStatus(0); // 投递中
        messageLog.setCount(0);  // 重试次数
        messageLog.setTryTime(LocalDateTime.now().plusMinutes(1000 * 60)); // 1分钟后重试
        messageLogService.insert(messageLog);

        try {
            //发送消息
            rabbitTemplate.convertAndSend("rule.exchange", "rule.update", ruleUpdateEvent,
                    new CorrelationData(msgId));
            log.info("规则更新消息发送成功，消息ID: {}", msgId);
        } catch (Exception e) {
            log.error("规则更新消息发送失败，消息ID: {}", msgId, e);
        }
    }
}
