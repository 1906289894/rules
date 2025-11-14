package com.wb.rules.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "drools_rules")
public class DroolsRules {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rule_key", unique = true)
    private String ruleKey;
    
    private String ruleName;
    @Lob
    private String ruleContent;
    private Integer version = 1;
    private Boolean status = true;
    private String description;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}