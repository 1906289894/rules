package com.wb.rules.service;

import com.wb.rules.entity.MessageLog;
import com.wb.rules.repository.MessageLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MessageLogService {
    
    @Autowired
    private MessageLogRepository messageLogRepository;
    
    public void insert(MessageLog messageLog) {
        messageLogRepository.save(messageLog);
    }
    
    public void updateStatus(String msgId, Integer status) {
        messageLogRepository.updateStatus(msgId, status);
    }
    
    public void updateCount(String msgId, LocalDateTime tryTime) {
        messageLogRepository.updateCount(msgId, tryTime);
    }
    
    public MessageLog getMessageById(String msgId) {
        return messageLogRepository.findByMsgId(msgId);
    }
    
    public List<MessageLog> getNeedRetryMessages() {
        return messageLogRepository.findNeedRetryMessages();
    }

    //    @Override
    //    public void recordProcessing(String msgId, String ruleVersion, String tenantId) {
    //        try {
    //            // 查询或创建消息记录
    //            MessageLog messageLog = messageLogRepository.findByMsgId(msgId)
    //                    .orElseGet(() -> createNewMessageLog(msgId, ruleVersion, tenantId));
    //
    //            // 更新处理状态
    //            messageLog.setStatus(MessageStatus.PROCESSING.getCode()); // 处理中
    //            messageLog.setCount(messageLog.getCount() + 1); // 重试次数+1
    //            messageLog.setUpdateTime(LocalDateTime.now());
    //            messageLog.setErrorMsg(null); // 清空错误信息
    //
    //            messageLogRepository.save(messageLog);
    //            log.debug("记录消息开始处理: {}", msgId);
    //
    //        } catch (Exception e) {
    //            log.error("记录消息处理状态失败: {}", msgId, e);
    //            throw new RuntimeException("记录消息处理状态失败", e);
    //        }
    //    }
    //
    //    @Override
    //    public void markSuccess(String msgId) {
    //        try {
    //            MessageLog messageLog = messageLogRepository.findByMsgId(msgId)
    //                    .orElseThrow(() -> new RuntimeException("消息记录不存在: " + msgId));
    //
    //            messageLog.setStatus(MessageStatus.SUCCESS.getCode()); // 成功
    //            messageLog.setUpdateTime(LocalDateTime.now());
    //            messageLog.setErrorMsg(null); // 清空错误信息
    //
    //            messageLogRepository.save(messageLog);
    //            log.debug("标记消息处理成功: {}", msgId);
    //
    //        } catch (Exception e) {
    //            log.error("标记消息成功失败: {}", msgId, e);
    //            throw new RuntimeException("标记消息成功失败", e);
    //        }
    //    }
    //
    //    @Override
    //    public void recordFailure(String msgId, String errorMsg) {
    //        try {
    //            MessageLog messageLog = messageLogRepository.findByMsgId(msgId)
    //                    .orElseGet(() -> {
    //                        log.warn("消息记录不存在，创建失败记录: {}", msgId);
    //                        return createNewMessageLog(msgId, "unknown", "unknown");
    //                    });
    //
    //            messageLog.setStatus(MessageStatus.FAILED.getCode()); // 失败
    //            messageLog.setUpdateTime(LocalDateTime.now());
    //            messageLog.setErrorMsg(truncateErrorMsg(errorMsg)); // 截断错误信息，避免过长
    //
    //            messageLogRepository.save(messageLog);
    //            log.debug("记录消息处理失败: {}, 错误: {}", msgId, errorMsg);
    //
    //        } catch (Exception e) {
    //            log.error("记录消息失败信息失败: {}", msgId, e);
    //            // 失败记录操作不抛出异常，避免影响主流程
    //        }
    //    }
    //
    //    @Override
    //    public void recordRetry(String msgId, String errorMsg) {
    //        try {
    //            MessageLog messageLog = messageLogRepository.findByMsgId(msgId)
    //                    .orElseGet(() -> {
    //                        log.warn("消息记录不存在，创建重试记录: {}", msgId);
    //                        return createNewMessageLog(msgId, "unknown", "unknown");
    //                    });
    //
    //            // 重试时状态仍为处理中，但记录错误信息
    //            messageLog.setStatus(MessageStatus.PROCESSING.getCode());
    //            messageLog.setUpdateTime(LocalDateTime.now());
    //            messageLog.setErrorMsg(truncateErrorMsg("重试: " + errorMsg));
    //
    //            messageLogRepository.save(messageLog);
    //            log.debug("记录消息重试: {}, 错误: {}", msgId, errorMsg);
    //
    //        } catch (Exception e) {
    //            log.error("记录消息重试信息失败: {}", msgId, e);
    //            // 重试记录操作不抛出异常，避免影响主流程
    //        }
    //    }
    //
    //    @Override
    //    public boolean isMessageProcessed(String msgId) {
    //        try {
    //            return messageLogRepository.findByMsgIdAndStatus(msgId, MessageStatus.SUCCESS.getCode())
    //                    .isPresent();
    //        } catch (Exception e) {
    //            log.error("检查消息是否已处理失败: {}", msgId, e);
    //            return false; // 查询失败时返回false，让业务继续处理
    //        }
    //    }
    //
    //    @Override
    //    public MessageLog getMessageById(String msgId) {
    //        try {
    //            return messageLogRepository.findByMsgId(msgId).orElse(null);
    //        } catch (Exception e) {
    //            log.error("根据消息ID查询消息记录失败: {}", msgId, e);
    //            return null;
    //        }
    //    }
    //
    //    @Override
    //    public void updateRetryCount(String msgId, int retryCount) {
    //        try {
    //            messageLogRepository.updateRetryCount(msgId, retryCount, LocalDateTime.now());
    //            log.debug("更新消息重试次数: {}, 次数: {}", msgId, retryCount);
    //        } catch (Exception e) {
    //            log.error("更新消息重试次数失败: {}", msgId, e);
    //        }
    //    }
    //
    //    /**
    //     * 创建新的消息记录
    //     */
    //    private MessageLog createNewMessageLog(String msgId, String ruleVersion, String tenantId) {
    //        MessageLog messageLog = new MessageLog();
    //        messageLog.setMsgId(msgId);
    //        messageLog.setRuleVersion(ruleVersion);
    //        messageLog.setTenantId(tenantId);
    //        messageLog.setStatus(MessageStatus.PROCESSING.getCode());
    //        messageLog.setCount(0);
    //        messageLog.setCreateTime(LocalDateTime.now());
    //        messageLog.setUpdateTime(LocalDateTime.now());
    //        return messageLog;
    //    }
    //
    //    /**
    //     * 截断错误信息，避免数据库字段过长
    //     */
    //    private String truncateErrorMsg(String errorMsg) {
    //        if (errorMsg == null) {
    //            return null;
    //        }
    //        return errorMsg.length() > 1000 ? errorMsg.substring(0, 1000) : errorMsg;
    //    }
}