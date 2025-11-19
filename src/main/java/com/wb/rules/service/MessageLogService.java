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
}