package com.leyou.item.api;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;


import org.springframework.web.bind.annotation.*;

import java.util.List;

//http://api.leyou.com/api/item/spu/page?key=&saleable=true&page=1&rows=5
@RequestMapping
public interface GoodsApi{

    @GetMapping("spu/page")
    public PageResult<SpuBo> querySpuByPage(@RequestParam(value = "key",required = false) String key,
                                                            @RequestParam(value = "saleable",required = false) boolean saleable,
                                                            @RequestParam(value = "page",defaultValue = "1") Integer page,
                                                            @RequestParam(value = "rows",defaultValue = "5") Integer rows);


    @GetMapping("spu/detail/{id}")
    public SpuDetail querySpuDetailBySpuId(@PathVariable("id") Long spuId);

    @GetMapping("sku/list")
    public List<Sku> querySkusBySpuId(@RequestParam("id") Long spuId);

    @GetMapping("spu/{id}")
    public Spu querySpuById(@PathVariable("id") Long id);

}
