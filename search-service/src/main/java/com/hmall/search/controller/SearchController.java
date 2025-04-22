package com.hmall.search.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.constants.ElasticConstants;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    //private final ISearchService searchService;

    private final RestHighLevelClient client;

    /*@ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) {
        // 分页查询
        Page<Item> result = searchService.lambdaQuery()
                .like(StrUtil.isNotBlank(query.getKey()), Item::getName, query.getKey())
                .eq(StrUtil.isNotBlank(query.getBrand()), Item::getBrand, query.getBrand())
                .eq(StrUtil.isNotBlank(query.getCategory()), Item::getCategory, query.getCategory())
                .eq(Item::getStatus, 1)
                .between(query.getMaxPrice() != null, Item::getPrice, query.getMinPrice(), query.getMaxPrice())
                .page(query.toMpPage("update_time", false));
        // 封装并返回
        return PageDTO.of(result, ItemDTO.class);
    }*/

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) throws IOException {
        SearchRequest request = new SearchRequest(ElasticConstants.ITEM_INDEX_NAME);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (query.getKey() != null && !query.getKey().isEmpty()) {
            boolQuery.must(QueryBuilders.matchQuery("name", query.getKey()));
        } else {
            boolQuery.must(QueryBuilders.matchAllQuery());
        }
        if (query.getBrand() != null) {
            boolQuery.filter(QueryBuilders.termQuery("brand.keyword", query.getBrand()));
        }
        if (query.getCategory() != null) {
            boolQuery.filter(QueryBuilders.termQuery("category.keyword", query.getCategory()));
        }
        if (query.getMinPrice() != null && query.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price")
                    .gte(query.getMinPrice()).lte(query.getMaxPrice()));
        }
        request.source().query(boolQuery);
        request.source().highlighter(SearchSourceBuilder.highlight().field("name"));

        request.source().from((query.getPageNo() - 1) * query.getPageSize()).size(query.getPageSize());
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        PageDTO<ItemDTO> result = parseResponseResult(response,query);
        return result;
    }

    private PageDTO<ItemDTO> parseResponseResult(SearchResponse response,ItemPageQuery query) {
        SearchHits searchHits = response.getHits();
        //4.1总条数
        long total = searchHits.getTotalHits().value;
        //System.out.println("total = " + total);
        //4.2命中的数据
        SearchHit[] hits = searchHits.getHits();
        List<ItemDTO> itemDTOList = new ArrayList<>();
        for (SearchHit hit : hits) {
            //4.2.1获取source结果
            String json = hit.getSourceAsString();
            //4.2.2转为ItemDoc
            ItemDoc doc = JSONUtil.toBean(json, ItemDoc.class);
            //4.2.3处理高亮结果
            Map<String, HighlightField> hfs = hit.getHighlightFields();
            if(hfs != null && !hfs.isEmpty()){
                //4.3.1 根据高亮字段名获取高亮结果
                HighlightField hf = hfs.get("name");
                //4.3.2 获取高亮结果，覆盖非高亮结果
                if (hf != null && hf.getFragments() != null && hf.getFragments().length > 0) {
                    String hfName = hf.getFragments()[0].toString();
                    doc.setName(hfName);
                }
            }
            // 4.2.4 将 ItemDoc 转换为 ItemDTO
            itemDTOList.add(BeanUtil.copyProperties(doc,ItemDTO.class));
            //System.out.println("doc = " + doc);
        }
        Page<ItemDTO> page = new Page<>();
        page.setRecords(itemDTOList);
        page.setTotal(total);
        page.setPages(total/query.getPageSize());
        return PageDTO.of(page);
    }



}
