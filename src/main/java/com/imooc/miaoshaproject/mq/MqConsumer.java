package com.imooc.miaoshaproject.mq;

import com.alibaba.fastjson.JSON;
import com.imooc.miaoshaproject.dao.ItemStockDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MqConsumer {

    private DefaultMQPushConsumer pullConsumer;


    @Autowired
    ItemStockDOMapper itemStockDOMapper;

    @Value("${rocketmq.producer.namesrvAddr}")
    private String namesrvAddr;

    @Value("${rocketmq.topicname}")
    private String topicName;


    @PostConstruct
    public void init() throws MQClientException {
        pullConsumer = new DefaultMQPushConsumer("stock_consumer_group");
        pullConsumer.setNamesrvAddr(namesrvAddr);
        pullConsumer.subscribe(topicName, "*");
        pullConsumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                Message msg = list.get(0);
                String jsonString = new String(msg.getBody());
                log.info("收到一条扣减库存信息：{}", jsonString);
                Map<String, Object> map = JSON.parseObject(jsonString, Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer amount = (Integer) map.get("amount");

                if (itemStockDOMapper.decreaseStock(itemId, amount) > 0) {
//                    throw new BusinessException( EmBusinessError.STOCK_NOT_ENOUGH);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        pullConsumer.start();
    }
}
