package com.wb.rules.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class RuleDlqConsumer {

    @RabbitListener(queues = "rule.dlq")
    public void handleDeadLetterMessage(@Payload String messageBody,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                        @Header(AmqpHeaders.CHANNEL) Channel channel,
                                        @Header(name = "x-death", required = false) List<Map<String, Object>> xDeath){
        try {
            log.warn("收到死信消息，消息体: {}", messageBody);

            if (xDeath != null) {
                // 可以获取消息成为死信的原因
                log.warn("消息成为死信的原因: {}", xDeath);
            }

            // 死信消息处理逻辑
            processDeadLetterMessage(messageBody);

            // 确认消息
            channel.basicAck(deliveryTag, false);
        }catch (Exception e){
            log.error("处理死信消息失败", e);
            try {
                // 死信队列处理失败也直接确认，避免无限循环
                channel.basicAck(deliveryTag, false);

                // 或者无线循环
                //// 所有持久化方式都失败，进入无限重试（不确认消息）
                //                log.error("死信消息持久化全部失败，消息将重新投递: {}", msgId);
                //                // 不进行ack/nack，让消息留在队列中重新投递
                //                Thread.sleep(60000); // 睡眠1分钟避免快速循环
            }catch (Exception e1){
                log.error("确认死信消息失败", e1);
            }
        }
    }

    private void processDeadLetterMessage(String messageBody) {
        // todo 死信消息处理：
        // 1. 尝试持久化到数据库（最高优先级）
        boolean dbSaved = saveToDatabase(message);

        // 2. 发送紧急告警
        boolean alertSent = sendEmergencyAlert(message);

        // 3. 记录到本地文件作为最后保障
        boolean fileSaved = saveToLocalFile(message);
        if (dbSaved || fileSaved) {
            // 只要有一种方式保存成功，就确认消息
            channel.basicAck(deliveryTag, false);
            log.info("死信消息处理完成: {}", msgId);
        } else {
            // 所有持久化方式都失败，进入无限重试（不确认消息）
            log.error("死信消息持久化全部失败，消息将重新投递: {}", msgId);
            // 不进行ack/nack，让消息留在队列中重新投递
            Thread.sleep(60000); // 睡眠1分钟避免快速循环
        }
        log.error("规则处理失败，需要人工干预，消息内容：{}", messageBody);
    }
}
