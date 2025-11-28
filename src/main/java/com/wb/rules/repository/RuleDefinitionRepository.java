package com.wb.rules.repository;

import com.wb.rules.entity.RuleDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleDefinitionRepository extends JpaRepository<RuleDefinition, Long> {
    
    List<RuleDefinition> findByStatusTrue();
    
    Optional<RuleDefinition> findByRuleKeyAndVersion(String ruleKey, String version);
    Optional<RuleDefinition> findTopByRuleKeyOrderByVersionDesc(String ruleKey);
    boolean existsByRuleKey(String ruleKey);
}