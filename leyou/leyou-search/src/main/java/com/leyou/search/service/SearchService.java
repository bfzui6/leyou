package com.leyou.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.api.SpecificationApi;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.reponsitory.GoodsRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired
    private GoodsRepository goodsRepository;

    public Goods buildGoods(Spu spu) throws Exception {
        Goods result = new Goods();
        //查分类名称
        List<String> names = categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        //查品牌名称
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        //设置all
        result.setAll(spu.getTitle()+" "+ StringUtils.join(names," ")+" "+brand.getName());
        //设置brandid
        result.setBrandId(spu.getBrandId());
        //..cid
        result.setCid1(spu.getCid1());
        result.setCid2(spu.getCid2());
        result.setCid3(spu.getCid3());
        //..
        result.setCreateTime(spu.getCreateTime());
        //..id
        result.setId(spu.getId());
        //..
        result.setSubTitle(spu.getSubTitle());
        //设置价格集合
        List<Long> prices = new ArrayList<>();
        List<Sku> skus1 = goodsClient.querySkusBySpuId(spu.getId());
        //new一个装skumap的list
        List<Map<String,Object>> skusList = new ArrayList<>();
        skus1.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String,Object> skumap = new HashMap<>();
            skumap.put("id",sku.getId());
            skumap.put("title",sku.getTitle());
            skumap.put("images",StringUtils.isNotBlank(sku.getImages())?
                    StringUtils.split(sku.getImages(),",")[0]:"");
            skumap.put("price",sku.getPrice());
            skusList.add(skumap);
        });
        result.setPrice(prices);
        //设置skus（反序列化的结果）
        String skus = MAPPER.writeValueAsString(skusList);
        result.setSkus(skus);
        //设置规格参数的map(参数名，值)
        //先查询出用于搜索的规格参数
        List<SpecParam> params = specificationClient.queryParams(null, spu.getCid3(), null, true);
        //通用规格参数
        SpuDetail spuDetail = goodsClient.querySpuDetailBySpuId(spu.getId());
        Map<Long,Object> generic = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<Long, Object>>() {
        });
        //特殊规格参数
        Map<Long,List<Object>> special = MAPPER.readValue(spuDetail.getSpecialSpec(),new TypeReference<Map<Long, List<Object>>>() {
        } );
        //定义map接收
        Map<String,Object> map = new HashMap<>();
        params.forEach(param -> {
            // 判断是否通用规格参数
            if (param.getGeneric()) {
                // 获取通用规格参数值
                String value = generic.get(param.getId()).toString();
                // 判断是否是数值类型
                if (param.getNumeric()){
                    // 如果是数值的话，判断该数值落在那个区间
                    value = chooseSegment(value, param);
                }
                // 把参数名和值放入结果集中
               map.put(param.getName(), value);
            } else {
                map.put(param.getName(), special.get(param.getId()));
            }
        });
        result.setSpecs(map);
        return result;
    }
    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public SearchResult search(SearchRequest request) {
        if (request.getKey() == null){
            return null;
        }
//        QueryBuilder queryBuilder = QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND);
        //自定义查询构造器
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        //添加搜索条件
        BoolQueryBuilder queryBuilder = buildBooleanQueryBuilder(request);
        nativeSearchQueryBuilder.withQuery(queryBuilder);
        //过滤结果 我们只要spuid subtitle skus
        nativeSearchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","skus","subTitle"},null));
        //分页
        nativeSearchQueryBuilder.withPageable(PageRequest.of(request.getPage()-1,request.getSize()));
        //聚合品牌与分类
        String categoryAggName = "categories";
        String brandsAggName = "brands";
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms(brandsAggName).field("brandId"));

        //查询结果
        AggregatedPage<Goods> page = (AggregatedPage<Goods>) goodsRepository.search(nativeSearchQueryBuilder.build());

        //解析结果集封装成List<Map<String, Object>> categories;   List<Brand> brands;
        List<Map<String, Object>> categories = getCategoryAggResult(page.getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(page.getAggregation(brandsAggName));
        //判断分类聚合是否为1  如果不是则不对规格参数进行聚合
        List<Map<String, Object>> specs = null;
        if (!CollectionUtils.isEmpty(categories) && categories.size()==1){
            specs = getParamAggResult((Long)categories.get(0).get("id"), queryBuilder);
        }

        SearchResult pageResult = new SearchResult();
        pageResult.setItems(page.getContent());
        pageResult.setTotal(page.getTotalElements());
        pageResult.setTotalPage(page.getTotalPages());
        pageResult.setBrands(brands);
        pageResult.setCategories(categories);
        pageResult.setSpecs(specs);
        return pageResult;
    }

    private BoolQueryBuilder buildBooleanQueryBuilder(SearchRequest request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //添加基本查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND));
        //判断有没有过滤条件
        if (CollectionUtils.isEmpty(request.getFilter())){
            return boolQueryBuilder;
        }
        //遍历filter
        for (Map.Entry<String, Object> stringObjectEntry : request.getFilter().entrySet()) {
            String key = stringObjectEntry.getKey();
            if (StringUtils.equals("品牌", key)) {
                key = "brandId";
            } else if (StringUtils.equals("分类", key)) {
                // 如果是“分类”，过滤字段名：cid3
                key = "cid3";
            } else {
                // 如果是规格参数名，过滤字段名：specs.key.keyword
                key = "specs." + key + ".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key,stringObjectEntry.getValue()));
        }

        return boolQueryBuilder;

    }

    private List<Map<String,Object>> getParamAggResult(Long id, QueryBuilder basicQuery) {

        // 创建自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 基于基本的查询条件，聚合规格参数
        queryBuilder.withQuery(basicQuery);
        // 查询要聚合的规格参数
        List<SpecParam> params = this.specificationClient.queryParams(null, id, null, true);
        // 添加聚合
        params.forEach(param -> {
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs." + param.getName() + ".keyword"));
        });
        // 只需要聚合结果集，不需要查询结果集
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{}, null));

        // 执行聚合查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());

        // 定义一个集合，收集聚合结果集
        List<Map<String, Object>> paramMapList = new ArrayList<>();
        // 解析聚合查询的结果集
        for (SpecParam specParam:params){
            StringTerms terms  = (StringTerms) goodsPage.getAggregation(specParam.getName());
            Map<String,Object> map = new HashMap<>();
            List<Object> options = new ArrayList<>();
            map.put("k",specParam.getName());
            terms.getBuckets().forEach(bucket -> {
                options.add(bucket.getKeyAsString());
            });
            map.put("options",options);
            paramMapList.add(map);
        }

//        Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();
//        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
//            Map<String, Object> map = new HashMap<>();
//            // 放入规格参数名
//            map.put("k", entry.getKey());
//            // 收集规格参数值
//            List<Object> options = new ArrayList<>();
//            // 解析每个聚合
//            StringTerms terms = (StringTerms)entry.getValue();
//            // 遍历每个聚合中桶，把桶中key放入收集规格参数的集合中
//            terms.getBuckets().forEach(bucket -> options.add(bucket.getKeyAsString()));
//            map.put("options", options);
//            paramMapList.add(map);
//        }

        return paramMapList;
    }

//    //聚合规格参数并解析结果集  封装成list<map>
//    private List<Map<String, Object>> getParamAggResult(Long id, QueryBuilder queryBuilder) {
//        //自定义查询器
//        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
//        //加入查询条件
//        nativeSearchQueryBuilder.withQuery(queryBuilder);
//        //得到规格参数集合
//        List<SpecParam> specParams = specificationClient.queryParams(null, id, null, true);
//        //遍历规格参数，添加聚合
//        specParams.forEach(specParam -> {
//            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms(specParam.getName()).field("specs." + specParam.getName() + ".keyword"));
//        });
//        //过滤
//        nativeSearchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{},null));
//        //查询  得到聚合结果
//        AggregatedPage<Goods> search = (AggregatedPage<Goods>) goodsRepository.search(nativeSearchQueryBuilder.build());
//        Map<String, Aggregation> stringAggregationMap = search.getAggregations().asMap();
//        List<Map<String, Object>> list = new ArrayList<>();
//        //遍历聚合结果集得到每个聚合,并且解析聚合结果封装到map中
//        Set<Map.Entry<String, Aggregation>> entries = stringAggregationMap.entrySet();
//        for (Map.Entry<String, Aggregation> entry:entries){
//            Map<String,Object> map = new HashMap<>();
//            map.put("k",entry.getKey());
//            List<Object> options = new ArrayList<>();
//            StringTerms value = (StringTerms) entry.getValue();
//            List<StringTerms.Bucket> buckets = value.getBuckets();
//            buckets.forEach(bucket -> {
//                options.add(bucket.getKeyAsString());
//            });
//            map.put("options",options);
//            //将map封装到list中
//            list.add(map);
//        }
//        return list;
//    }

    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        LongTerms longTerms = (LongTerms)aggregation;
        return longTerms.getBuckets().stream().map(bucket -> {
            long l = bucket.getKeyAsNumber().longValue();
            return this.brandClient.queryBrandById(l);
        }).collect(Collectors.toList());
    }
    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {
        LongTerms longTerms = (LongTerms)aggregation;
        return longTerms.getBuckets().stream().map(bucket -> {
            Map<String,Object> map = new HashMap<>();
            long l = bucket.getKeyAsNumber().longValue();
            List<String> names = categoryClient.queryNamesByIds(Arrays.asList(l));
            map.put("id",l);
            map.put("name",names.get(0));
            return map;
        }).collect(Collectors.toList());
    }

    public void save(Long id) throws Exception{
        Spu spu = goodsClient.querySpuById(id);
        Goods goods = buildGoods(spu);
        goodsRepository.save(goods);
    }
}
