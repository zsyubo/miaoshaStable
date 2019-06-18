package com.imooc.miaoshaproject.mq;

import com.alibaba.fastjson.JSON;
import com.imooc.miaoshaproject.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class MqProducer {

    private DefaultMQProducer defaultMQProducer;

    private TransactionMQProducer transactionMQProducer;


    @Autowired
    OrderService orderService;

    @Value("${rocketmq.producer.namesrvAddr}")
    private String namesrvAddr;

    @Value("${rocketmq.topicname}")
    private String topicName;

    @PostConstruct
    public void init() throws MQClientException {
        defaultMQProducer = new DefaultMQProducer("produce_group");
        defaultMQProducer.setNamesrvAddr(namesrvAddr);
        defaultMQProducer.start();

        // 事务消息
        transactionMQProducer = new TransactionMQProducer("transaction_produce_group");
        transactionMQProducer.setNamesrvAddr(namesrvAddr);
        transactionMQProducer.start();
        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object o) {
                // 执行本地事务消息

                Map<String, Object> args = (Map<String, Object>) o;
                Integer userId = (Integer) args.get("userId");
                Integer promoId = (Integer) args.get("promoId");
                Integer itemId = (Integer) args.get("itemId");
                Integer amount = (Integer) args.get("amount");
                String stockLogId = args.get("stockLogId").toString();
                try {
                    // 真正需要做的是 创建订单
                    orderService.createOrder(userId, itemId, promoId, amount, stockLogId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
                // 根据是否扣减库存是否成功来判断是否要返回 ROLLBACK_MESSAGE，commit。unknown
//                String  jsonString = new String(messageExt.getBody());
//
//                Map<String, Object> map =  JSON.parseObject(jsonString, Map.class);
//                Integer itemId = (Integer) map.get("itemId");
//                Integer amount = (Integer) map.get("amount");
                System.out.println("checkLocalTransaction：此方法执行了");
                return null;
            }
        });
    }

    //
    @Deprecated
    /**
     * 事务型同步库存扣减消息
     * @param stockLogId  扣减库存log id
     * @param userId 用户id
     * @param promoId  秒杀id
     * @param itemId  秒杀库id
     * @param amount  秒杀数量
     * @return boolean
     * @author hyf
     * @date 2019-06-18
     */
    public boolean transactionAsyncReduceStock(String stockLogId, Integer userId, Integer promoId, Integer itemId, Integer amount) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        bodyMap.put("stockLogId", stockLogId);
        bodyMap.put("userId", userId);
        bodyMap.put("promoId", promoId);

        String msg = JSON.toJSON(bodyMap).toString();

        log.info("发送一条扣减库存信息：{}", msg);
        Message message = new Message(topicName, "increase", msg.getBytes(Charset.forName("UTF-8")));
        TransactionSendResult transactionSendResult = null;
        try {
            transactionSendResult = transactionMQProducer.sendMessageInTransaction(message, bodyMap);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }
        // 判断事务消息是否提交成功
        if (transactionSendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
            return false;

        } else if (transactionSendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE) {
            return true;
        } else {
            return false;
        }
    }


    // 同步库存扣减消息
    public boolean asyncReduceStock(Integer itemId, Integer amount) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);

        String msg = JSON.toJSON(bodyMap).toString();

        log.info("发送一条扣减库存信息：{}", msg);
        Message message = new Message(topicName, "increase", msg.getBytes(Charset.forName("UTF-8")));
        try {
            defaultMQProducer.send(message);
            return true;
        } catch (MQClientException e) {
            return false;
        } catch (RemotingException e) {
            return false;
        } catch (MQBrokerException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
