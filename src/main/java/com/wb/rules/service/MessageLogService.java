package com.wb.rules.service;

import cn.hutool.core.util.StrUtil;
import com.wb.rules.common.enums.MessageStatus;
import com.wb.rules.entity.MessageLog;
import com.wb.rules.event.RuleUpdateEvent;
import com.wb.rules.repository.MessageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageLogService {

    private final MessageLogRepository messageLogRepository;

    /**
     * 查询消息是否处理
     *
     * @param msgId 消息id
     * @return flag
     */
    public boolean isMessageProcessed(String msgId) {
        try {
            return Objects.nonNull(messageLogRepository.findByMsgIdAndStatus(msgId, 1));
        } catch (Exception e) {
            log.error("检查消息是否已处理失败: {}", msgId, e);
            return false;
        }
    }

    /**
     * 消息发送失败，正在重试中
     *
     * @param msgId    消息id
     * @param errorMsg 失败原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordRetry(String msgId, String errorMsg) {
        try {
            MessageLog messageLog = messageLogRepository.findByMsgId(msgId);
            if (Objects.isNull(messageLog)) {
                log.warn("消息记录不存在，创建重试记录: {}", msgId);
                messageLog = createNewMessageLog(msgId, "unknown", "unknown");
            }

            // 重试时状态仍为处理中，但记录错误信息
            messageLog.setStatus(0);
            messageLog.setUpdateTime(LocalDateTime.now());
            messageLog.setErrorMsg(truncateErrorMsg("重试: " + errorMsg));

            messageLogRepository.save(messageLog);
            log.debug("记录消息重试: {}, 错误: {}", msgId, errorMsg);

        } catch (Exception e) {
            log.error("记录消息重试信息失败: {}", msgId, e);
            // 重试记录操作不抛出异常，避免影响主流程
        }
    }

    /**
     * 创建新的消息记录
     */
    private MessageLog createNewMessageLog(String msgId, String ruleVersion, String ruleKey) {
        MessageLog messageLog = new MessageLog();
        messageLog.setMsgId(msgId);
        messageLog.setRuleVersion(ruleVersion);
        messageLog.setRuleKey(ruleKey);
        messageLog.setStatus(MessageStatus.PROCESSING.getCode());
        messageLog.setCount(0);
        messageLog.setCreateTime(LocalDateTime.now());
        messageLog.setUpdateTime(LocalDateTime.now());
        return messageLog;
    }

    /**
     * 记录发送失败的消息
     *
     * @param msgId           消息id
     * @param errorMsg        消息发送失败原因
     * @param ruleUpdateEvent 规则
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordFailure(String msgId, String errorMsg, RuleUpdateEvent ruleUpdateEvent) {
        try {
            MessageLog messageLog = messageLogRepository.findByMsgId(msgId);
            String ruleKey = StrUtil.isBlank(ruleUpdateEvent.getRuleKey()) ? "unknown" : ruleUpdateEvent.getRuleKey();
            String ruleVersion = StrUtil.isBlank(ruleUpdateEvent.getRuleVersion()) ? "unknown" : ruleUpdateEvent.getRuleVersion();
            if (Objects.isNull(messageLog)) {
                log.warn("消息记录不存在，创建失败记录: {}", msgId);
                messageLog = createNewMessageLog(msgId, ruleVersion, ruleKey);
            }
            messageLog.setStatus(MessageStatus.FAILED.getCode()); // 失败
            messageLog.setUpdateTime(LocalDateTime.now());
            messageLog.setErrorMsg(truncateErrorMsg(errorMsg)); // 截断错误信息，避免过长

            messageLogRepository.save(messageLog);
            log.debug("记录消息处理失败: {}, 错误: {}", msgId, errorMsg);

        } catch (Exception e) {
            log.error("记录消息失败信息失败: {}", msgId, e);
            // 失败记录操作不抛出异常，避免影响主流程
        }
    }

    /**
     * 截断错误信息，避免数据库字段过长
     */
    private String truncateErrorMsg(String errorMsg) {
        if (errorMsg == null) {
            return null;
        }
        return errorMsg.length() > 1000 ? errorMsg.substring(0, 1000) : errorMsg;
    }


    public void recordSuccess(String msgId, RuleUpdateEvent ruleUpdateEvent) {
        MessageLog messageLog = messageLogRepository.findByMsgId(msgId);
        if (Objects.isNull(messageLog)) {
            messageLog = createNewMessageLog(msgId, ruleUpdateEvent.getRuleVersion(), ruleUpdateEvent.getRuleKey());
        }

        messageLog.setStatus(MessageStatus.SUCCESS.getCode());
        messageLog.setUpdateTime(LocalDateTime.now());
        messageLog.setErrorMsg(null); // 清空错误信息
        messageLogRepository.save(messageLog);
    }
}