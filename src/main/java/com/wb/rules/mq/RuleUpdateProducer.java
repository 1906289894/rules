package com.wb.rules.mq;

import cn.hutool.core.util.StrUtil;
import com.wb.rules.common.exceptions.RuleException;
import com.wb.rules.dto.RuleMessageSendResult;
import com.wb.rules.event.RuleUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleUpdateProducer {
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送规则更新消息
     */
    public RuleMessageSendResult sendRuleUpdateMessage(RuleUpdateEvent ruleUpdateEvent) {
        String msgId = UUID.randomUUID().toString();

        try {
            // 验证消息参数
            validateRuleUpdateEvent(ruleUpdateEvent);

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

            // 更新消息状态为已发送（这里依赖confirm回调来更新状态）
            log.info("规则消息发送成功: ID={}, Rule={}, Version={}", msgId, ruleUpdateEvent.getRuleKey(), ruleUpdateEvent.getRuleVersion());
            return RuleMessageSendResult.builder()
                    .sendStatus(true)
                    .msgKey(ruleUpdateEvent.getRuleKey())
                    .msgId(msgId)
                    .build();

        } catch (Exception e) {
            log.error("规则消息发送失败: Rule={}, Version={}", ruleUpdateEvent.getRuleKey(), ruleUpdateEvent.getRuleVersion(), e);
            return RuleMessageSendResult.builder()
                    .sendStatus(true)
                    .msgKey(ruleUpdateEvent.getRuleKey())
                    .msgId(msgId)
                    .errorMsg(e.getMessage())
                    .build();
        }
    }

    /**
     * 批量发送规则更新
     */
    public List<RuleMessageSendResult> batchSendRuleUpdates(List<RuleUpdateEvent> events){
        return events.stream()
                .map(this::sendRuleUpdateMessage)
                .collect(Collectors.toList());
    }

    /**
     * 验证消息参数
     */
    static void validateRuleUpdateEvent(RuleUpdateEvent event) {
        if (Objects.isNull(event)) {
            throw new RuleException("规则更新事件不能为空");
        }
        if (StrUtil.isBlank(event.getRuleKey())){
            throw new RuleException("规则key不可为空");
        }
        if (StrUtil.isBlank(event.getRuleVersion())) {
            throw new RuleException("规则版本不能为空");
        }
        if (StrUtil.isBlank(event.getRuleContent())) {
            log.warn("规则内容为空，消费者将尝试从存储加载");
        }
    }
}