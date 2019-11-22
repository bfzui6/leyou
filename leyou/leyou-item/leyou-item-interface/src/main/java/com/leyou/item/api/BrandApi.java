package com.leyou.item.api;

import com.leyou.item.pojo.Brand;


import org.springframework.web.bind.annotation.*;




//http://api.leyou.com/api/item/brand/page?key=&page=1&rows=5&sortBy=id&desc=false
@RequestMapping("brand")
public interface BrandApi {

    @GetMapping("{bid}")
    public Brand queryBrandById(@PathVariable("bid") Long bid);

}
