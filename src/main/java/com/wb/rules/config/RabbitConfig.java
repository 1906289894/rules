package com.wb.rules.config;

import com.wb.rules.service.MessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.util.HashMap;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RabbitConfig {

    //todo 这里需要优化
    @Value("${app.mq.max-retry-count:3}")
    private int maxRetryCount;

    @Value("${app.mq.message-ttl:10000}")
    private int messageTtl;
    @Value("${app.mq.retry.initial-interval:1000}")
    private int initialInterval;

    @Value("${app.mq.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${app.mq.retry.max-interval:10000}")
    private int maxInterval;


    private final CachingConnectionFactory connectionFactory;
    private final MessageLogService messageLogService;

    // 使用Jackson2JsonMessageConverter替代默认的SimpleMessageConverter
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String msgId = correlationData != null ? correlationData.getId() : "unknown";
            if (ack){
                //messageLogService.updateStatus(msgId, 1); // 更新状态为成功
                log.info("消息 {} 发送成功", msgId);
            }else{
                log.error("消息 {} 发送失败，原因: {}", msgId, cause);
            }
        });
        return rabbitTemplate;
    }

    //声明更新规则队列
    @Bean
    public Queue ruleUpdateQueue() {
        HashMap<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "rule.dlx.exchange"); // 死信交换机
        args.put("x-dead-letter-routing-key", "rule.dlx.routingkey"); // 死信路由键
        args.put("x-message-ttl", messageTtl); //消息ttl 10s
        return new Queue("rule.update.queue", true, false, false, args);
    }

    //声明死信队列
    @Bean
    public Queue ruleDlq(){
        return new Queue("rule.dlq", true);
    }

    @Bean
    public DirectExchange ruleExchange() {
        return new DirectExchange("rule.exchange", true, false);
    }

    @Bean
    public DirectExchange ruleDlxExchange() {
        return new DirectExchange("rule.dlx.exchange", true, false);
    }

    @Bean
    public Binding ruleBinding(){
        return BindingBuilder.bind(ruleUpdateQueue()).to(ruleExchange()).with("rule.update");
    }

    @Bean
    public Binding ruleDlxBinding(){
        return BindingBuilder.bind(ruleDlq()).to(ruleDlxExchange()).with("rule.dlx.routingkey");
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        factory.setAdviceChain(retryInterceptor());
        return factory;
    }

    @Bean
    public RetryOperationsInterceptor retryInterceptor(){
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(maxRetryCount)//最大重试次数
                .backOffOptions(initialInterval, multiplier, maxInterval) // 重试间隔策略
                .recoverer(new RejectAndDontRequeueRecoverer()) // 最终失败时拒绝并不重新入队（进入死信队列）
                .build();
    }
}
