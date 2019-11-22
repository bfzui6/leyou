package com.leyou.goodsweb.controller;

import com.leyou.goodsweb.service.GoodsService;
import com.leyou.goodsweb.service.HtmlGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("item")
public class GoodsController {
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private HtmlGoodsService htmlGoodsService;
    @GetMapping("{id}.html")
    public String toItemPage(@PathVariable("id") Long id, Model model){
        System.out.println("controller");
        Map<String, Object> map = goodsService.loadData(id);
        model.addAllAttributes(map);
        htmlGoodsService.createHtml(id);
        return "item";
    }
}
