package com.leyou.search.listener;

import com.leyou.search.service.SearchService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoodsListener {
    @Autowired
    private  SearchService searchService;
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "LEYOU.CREATE.INDEX.QUEUE",durable = "true"),
    exchange = @Exchange(value = "LEYOU.ITEM.EXCHANGE",ignoreDeclarationExceptions = "true",type = "topic"),
    key = {"item.insert", "item.update"}))
    public void save(Long id) throws Exception {
        if (id == null){
            return;
        }
        searchService.save(id);
    }
}
