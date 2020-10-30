package com.seckillproject.mq;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConsumer {
    //队列bean的名称
    public static final String QUEUE_ITEM_BLOCK = "queue_item_stock";
    //交换机Bean的名称
    public static final String EX_ROUTING_ITEM_STOCK="ex_routing_item_stock";
    //队列的名称
    @Value("${seckill.mq.queue}")
    public  String queue_item_stock_name;
    //routingKey
    @Value("${seckill.mq.routingKey}")
    public  String routingKey;
    /**
     * 交换机配置使用direct类型
     * @return the exchange
     */
    @Bean(EX_ROUTING_ITEM_STOCK)
    public Exchange EXCHANGE_TOPICS_INFORM() {
        return ExchangeBuilder.directExchange(EX_ROUTING_ITEM_STOCK).durable(true).build();
    }
    //声明队列
    @Bean(QUEUE_ITEM_BLOCK)
    public Queue QUEUE_CMS_POSTPAGE() {
        Queue queue = new Queue(queue_item_stock_name);
        return queue;
    }
    /**
     * 绑定队列到交换机
     * @param queue    the queue
     * @param exchange the exchange
     * @return the binding
     */
    @Bean
    public Binding BINDING_QUEUE_INFORM_SMS(@Qualifier(QUEUE_ITEM_BLOCK) Queue queue, @Qualifier(EX_ROUTING_ITEM_STOCK) Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs();
    }

}
