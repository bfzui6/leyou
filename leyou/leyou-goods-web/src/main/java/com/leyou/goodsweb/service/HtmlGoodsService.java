package com.leyou.goodsweb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Service
public class HtmlGoodsService {
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private TemplateEngine templateEngine;
    public void createHtml(Long spuId){
        Map map = null;
        map = goodsService.loadData(spuId);
        Context context = new Context();
        context.setVariables(map);
        File file = new File("E:\\nginx\\nginx-1.14.0\\nginx-1.14.0\\html\\item\\"+spuId + ".html");
        PrintWriter printWriter =null;
        try {
            printWriter = new PrintWriter(file);
            templateEngine.process("item",context,printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (printWriter !=null){
                printWriter.close();
            }
        }

    }
}
