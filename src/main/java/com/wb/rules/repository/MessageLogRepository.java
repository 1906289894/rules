package com.wb.rules.repository;

import com.wb.rules.entity.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, String> {
    
    @Modifying
    @Query("UPDATE MessageLog SET status = :status, updateTime = CURRENT_TIMESTAMP WHERE msgId = :msgId")
    int updateStatus(@Param("msgId") String msgId, @Param("status") Integer status);
    
    @Modifying
    @Query("UPDATE MessageLog SET count = count + 1, tryTime = :tryTime, updateTime = CURRENT_TIMESTAMP WHERE msgId = :msgId")
    int updateCount(@Param("msgId") String msgId, @Param("tryTime") LocalDateTime tryTime);
    
    @Query("SELECT m FROM MessageLog m WHERE m.status = 0 AND m.count < 3 AND m.tryTime <= CURRENT_TIMESTAMP")
    List<MessageLog> findNeedRetryMessages();
    
    @Query("SELECT m FROM MessageLog m WHERE m.msgId = :msgId")
    MessageLog findByMsgId(@Param("msgId") String msgId);

    @Query("SELECT m FROM MessageLog m where m.msgId = :msgId and m.status = :status")
    MessageLog findByMsgIdAndStatus(String msgId, int status);
}