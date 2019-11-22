package com.leyou.goodsweb.service;

import com.leyou.goodsweb.client.BrandClient;
import com.leyou.goodsweb.client.CategoryClient;
import com.leyou.goodsweb.client.GoodsClient;
import com.leyou.goodsweb.client.SpecificationClient;
import com.leyou.item.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoodsService {
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;

    public Map<String, Object> loadData(Long spuId){
        //初始化一个map
        Map<String,Object> model = new HashMap<>();
        //查询spu
        Spu spu = goodsClient.querySpuById(spuId);

        //差品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());

        //查spudetail
        SpuDetail spuDetail = goodsClient.querySpuDetailBySpuId(spuId);

        //查skus
        List<Sku> skus = goodsClient.querySkusBySpuId(spuId);

        //查分类  并且将结果封装成list<map<string,object>>categories
        List<Map<String,Object>> cateList = new ArrayList<>();
        List<Long> cids = Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3());
        List<String> names = categoryClient.queryNamesByIds(cids);
        for (int i = 0; i < cids.size(); i++) {
            Map<String,Object> map =new HashMap<>();
            map.put("id",cids.get(i));
            map.put("name",names.get(i));
            cateList.add(map);
        }

        //查询规格参数组
        List<SpecGroup> specGroups = specificationClient.querySpecsByCid(spu.getCid3());

        //查询特殊规格参数 并封装为map<long,string>
        Map<Long,String> map = new HashMap<>();
        List<SpecParam> specParams = specificationClient.queryParams(null, spu.getCid3(), false, null);
        specParams.forEach(param->{
            map.put(param.getId(),param.getName());
        });


        //spu
        model.put("spu",spu);
        //spuDetail
        model.put("spuDetail",spuDetail);
        //skus
        model.put("skus",skus);
        //categories
        model.put("categories",cateList);
        //groups
        model.put("groups",specGroups);
        // 查询特殊规格参数
        model.put("paramMap", map);
        //brand
        model.put("brand",brand);
        return model;
    }
}
