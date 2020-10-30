package com.seckillproject.mq;

import com.rabbitmq.client.AMQP;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class MqProducer {
    //交换机的名称
    public static final String EX_ROUTING_ITEM_STOCK = "ex_routing_item_stock";
    /**
     * 交换机配置使用direct类型
     * @return the exchange
     */
    @Bean(EX_ROUTING_ITEM_STOCK)
    public AMQP.Exchange EXCHANGE_TOPICS_INFORM() {
        return ExchangeBuilder.directExchange(EX_ROUTING_ITEM_STOCK).durable(true).build();
    }
}
