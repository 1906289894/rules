package com.wb.rules.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@Entity
@Table(name = "mail_send_log")
@NoArgsConstructor
@AllArgsConstructor
public class MessageLog {
    @Id
    @Column(name = "msg_id", length = 64)
    private String msgId;
    
    @Column(name = "rule_version", length = 50)
    private String ruleVersion;
    
    @Column(name = "status")
    private Integer status = 0; // 0:投递中;1:投递成功;2:投递失败

    @Column(name = "rule_key", unique = true)
    private String ruleKey;
    
    @Column(name = "count")
    private Integer count = 0; // 重试次数
    
    @Column(name = "try_time")
    private LocalDateTime tryTime;
    
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime = LocalDateTime.now();
    
    @Column(name = "update_time")
    private LocalDateTime updateTime = LocalDateTime.now();

    @Column(name = "error_msg", length = 1000)
    private String errorMsg;
}