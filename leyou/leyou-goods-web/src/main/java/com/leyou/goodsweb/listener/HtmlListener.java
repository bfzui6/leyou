package com.leyou.goodsweb.listener;

import com.leyou.goodsweb.service.GoodsService;
import com.leyou.goodsweb.service.HtmlGoodsService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HtmlListener {
    @Autowired
    private HtmlGoodsService htmlgoodsService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "LEYOU.CREATE.WEB.QUEUE", durable = "true"),
            exchange = @Exchange(
                    value = "LEYOU.ITEM.EXCHANGE",
                    ignoreDeclarationExceptions = "true",
                    type = ExchangeTypes.TOPIC),
            key = {"item.insert", "item.update"}))
    public void save(Long id){
        if (id == null){
            return;
        }
        htmlgoodsService.createHtml(id);
    }
}
