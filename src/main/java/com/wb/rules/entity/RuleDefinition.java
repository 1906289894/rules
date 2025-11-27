package com.wb.rules.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rule_definition")
public class RuleDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rule_key", unique = true)
    private String ruleKey;
    
    private String ruleName;
    @Lob
    private String ruleContent;
    private String version;
    private Boolean status = true;
    private String description;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}