package com.leyou.elasticsearch.test;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.reponsitory.GoodsRepository;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ElasticSearchTest {
    @Autowired
    private GoodsRepository repository;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;
    @Autowired
    private SearchService searchService;

    @Test
    public void testElasticsearch() {

        Integer page = 1;
        Integer rows = 100;
        //创建索引及映射
        elasticsearchTemplate.createIndex(Goods.class);
        elasticsearchTemplate.putMapping(Goods.class);
        do {
            //分页查询spu
            PageResult<SpuBo> pageResult = goodsClient.querySpuByPage(null, true, page, rows);
            List<SpuBo> items = pageResult.getItems();
            //将spulist转换为goodslist
            List<Goods> goodsList = items.stream().map(spuBo -> {
                try {
                    return searchService.buildGoods(spuBo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());
            //导入数据
            repository.saveAll(goodsList);

            page++;
            rows = items.size();
        } while (rows == 100);
    }
}
