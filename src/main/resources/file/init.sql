CREATE TABLE `drools_rules` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                `rule_key` varchar(64) UNIQUE NOT NULL COMMENT '规则唯一标识',
                                `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
                                `rule_content` text NOT NULL COMMENT 'DRL规则内容',
                                `version` int(11) DEFAULT '1' COMMENT '版本号',
                                `status` tinyint(1) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
                                `description` varchar(500) DEFAULT NULL COMMENT '规则描述',
                                `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_rule_key` (`rule_key`)
) ENGINE=InnoDB COMMENT='Drools规则表';

-- 插入示例规则
INSERT INTO `drools_rules` (`rule_key`, `rule_name`, `rule_content`, `description`) VALUES
    ('ORDER_SCORE_RULE', '订单积分规则', '
package com.rules.order
import com.example.drools.entity.Order

rule "订单积分规则-100以下"
when
    $order: Order(amount < 100)
then
    $order.setScore(0.0);
    $order.setMessage("100元以下不加分");
end

rule "订单积分规则-100到500"
when
    $order: Order(amount >= 100 && amount < 500)
then
    $order.setScore(100.0);
    $order.setMessage("100-500元加100分");
end
', '订单金额对应的积分计算规则');