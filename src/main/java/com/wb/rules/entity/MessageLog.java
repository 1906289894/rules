package com.wb.rules.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mail_send_log")
public class MessageLog {
    @Id
    @Column(name = "msg_id", length = 64)
    private String msgId;
    
    @Column(name = "rule_version", length = 50)
    private String ruleVersion;
    
    @Column(name = "status")
    private Integer status = 0; // 0:投递中;1:投递成功;2:投递失败
    
    @Column(name = "count")
    private Integer count = 0; // 重试次数
    
    @Column(name = "try_time")
    private LocalDateTime tryTime;
    
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime = LocalDateTime.now();
    
    @Column(name = "update_time")
    private LocalDateTime updateTime = LocalDateTime.now();
}