package com.seckillproject.mq;

import com.alibaba.fastjson.JSON;
import com.seckillproject.error.BusinessException;
import com.seckillproject.error.EmBusinessError;
import com.seckillproject.mapper.ItemStockMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MqConsumerComponent {
    @Autowired
    ItemStockMapper itemStockMapper;

    @RabbitListener(queues = {"${seckill.mq.queue}"})
    public void receiveMessageFromMq(String msg) {
        //解析消息
        Map map = JSON.parseObject(msg, Map.class);
        Integer itemId = (Integer) map.get("itemId");
        Integer amount = (Integer) map.get("amount");
        itemStockMapper.decreaseStock(itemId,amount);
    }
}
