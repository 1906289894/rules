package com.wb.rules.repository;

import com.wb.rules.entity.DroolsRules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DroolsRulesRepository extends JpaRepository<DroolsRules, Long> {
    
    List<DroolsRules> findByStatusTrue();
    
    Optional<DroolsRules> findByRuleKeyAndStatusTrue(String ruleKey);
    
    boolean existsByRuleKey(String ruleKey);
}