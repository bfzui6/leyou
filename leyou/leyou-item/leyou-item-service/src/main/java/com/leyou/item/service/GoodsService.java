package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.mapper.*;

import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;

import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.pojo.Stock;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoodsService {
    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private SpuDetailMapper spuDetailMapper;
    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private AmqpTemplate amqpTemplate;


    public PageResult<SpuBo> querySpuByPage(String key, Boolean saleable, Integer page, Integer rows) {
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //查询条件
        if (StringUtils.isNotEmpty(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        //过滤条件
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        //分页
        PageHelper.startPage(page, rows);

        //查询结果得到list<spu>
        List<Spu> spus = spuMapper.selectByExample(example);
        PageInfo<Spu> pageInfo = new PageInfo<>(spus);
        //转换成list<spubo>
        List<SpuBo> spuBos = spus.stream().map(spu -> {
            SpuBo spuBo = new SpuBo();
            BeanUtils.copyProperties(spu, spuBo);
            //查品牌
            spuBo.setBname(brandMapper.selectByPrimaryKey(spu.getBrandId()).getName());
            //查分类
            List<String> names = categoryService.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            String name = StringUtils.join(names, "-");
            spuBo.setCname(name);
            return spuBo;
        }).collect(Collectors.toList());
        //封装成resule<spubo>
        PageResult<SpuBo> pageResult = new PageResult<>(pageInfo.getTotal(), spuBos);
        return pageResult;
    }

    public void saveGoods(SpuBo spuBo){
        //新增spu
        spuBo.setId(null);
        spuBo.setSaleable(true);
        spuBo.setValid(true);
        spuBo.setCreateTime(new Date());
        spuBo.setLastUpdateTime(spuBo.getCreateTime());
        spuMapper.insertSelective(spuBo);
        //新增spudetail
        SpuDetail spuDetail = spuBo.getSpuDetail();
        spuDetail.setSpuId(spuBo.getId());
        spuDetailMapper.insertSelective(spuDetail);
        //新增sku
        saveSkuAndStock(spuBo);
        sendMsg("insert",spuBo.getId());
    }

    private void sendMsg(String type,Long id) {
        try {
            amqpTemplate.convertAndSend("item."+type,id);
        } catch (AmqpException e) {
            e.printStackTrace();
        }
    }

    private void saveSkuAndStock(SpuBo spuBo) {
        List<Sku> skus = spuBo.getSkus();
        skus.forEach(sku -> {
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(new Date());
            sku.setSpuId(spuBo.getId());
            skuMapper.insertSelective(sku);
            //新增stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockMapper.insert(stock);
        });
    }

    public SpuDetail querySpuDetailBySpuId(Long spuId){
        SpuDetail spuDetail = null;
        if (spuId !=null){
            spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        }
        return spuDetail;
    }

    public List<Sku> querySkusBySpuId(Long spuId){
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skus = skuMapper.select(sku);
        if (!CollectionUtils.isEmpty(skus)){
            skus.forEach(sku1 -> {
                Stock stock = stockMapper.selectByPrimaryKey(sku1.getId());
                if (stock!=null){
                    sku1.setStock(stock.getStock());
                }

            });
        }
        return skus;
    }
    public void updateGoods(SpuBo spuBo){
            Sku s = new Sku();
            s.setSpuId(spuBo.getId());
            List<Sku> skus = skuMapper.select(s);
            //删除stock
            skus.forEach(sku -> {
                        stockMapper.deleteByPrimaryKey(sku.getId());
                    }
            );
            //删除sku
            Sku sku = new Sku();
            sku.setSpuId(spuBo.getId());
            skuMapper.delete(sku);
            //增加sku
            //增加stock
            this.saveSkuAndStock(spuBo);
            //更新spu
            Spu spu = new Spu();
            spu.setId(spuBo.getId());
            spu.setCreateTime(null);
            spu.setLastUpdateTime(new Date());
            spu.setSaleable(null);
            spu.setValid(null);
            spuMapper.updateByPrimaryKeySelective(spu);
            //更新spudetail
            spuDetailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());
            sendMsg("update",spuBo.getId());


    }

    public Spu querySpuById(Long id) {
        return  this.spuMapper.selectByPrimaryKey(id);
    }
}
