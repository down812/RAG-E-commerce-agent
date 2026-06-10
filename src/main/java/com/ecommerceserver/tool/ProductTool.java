package com.ecommerceserver.tool;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerceserver.mapper.ProductMapper;
import com.ecommerceserver.model.dto.ProductToolResult;
import com.ecommerceserver.model.entity.Product;
import com.ecommerceserver.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProductTool {
    private final ProductService productService;
    private final ProductMapper productMapper;

    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);
    private final ConcurrentHashMap<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    private static class CacheEntry<T> {
        final T data;
        final long expireAt;
        CacheEntry(T data, long ttlMs) {
            this.data = data;
            this.expireAt = System.currentTimeMillis() + ttlMs;
        }
        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getCache(String key) {
        CacheEntry<?> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return (T) entry.data;
        }
        if (entry != null) {
            cache.remove(key, entry);
        }
        return null;
    }

    private <T> void putCache(String key, T data) {
        cache.put(key, new CacheEntry<>(data, CACHE_TTL_MS));
    }

    @Tool(description = "【必须调用】根据商品ID查询商品详情信息。" +
            "当你需要获取任何商品的名称、价格、品牌、分类、销量、状态等数据时，必须调用此工具，禁止凭记忆回答。" +
            "返回字段说明：id(商品ID), productCode(商品编码), title(商品名称), brand(品牌), category(一级分类), subCategory(二级分类), basePrice(价格), salesCount(销量), status(1=上架/0=下架), mainImageUrl(主图URL)。" +
            "适用于：用户想了解某个具体商品的详细信息，或需要验证商品数据时。")
    public List<ProductToolResult> getProductByIds(@ToolParam(required = false, description = "商品ID列表，例如：[1, 2, 3]，可传入单个ID或多个ID") List<Long> productIds) {
        if (CollectionUtil.isNotEmpty(productIds)) {
            List<Long> sortedIds = productIds.stream().distinct().sorted().toList();
            String cacheKey = "ids:" + sortedIds;
            List<ProductToolResult> cached = getCache(cacheKey);
            if (cached != null) {
                return cached;
            }
            List<Product> result = productService.listByIds(sortedIds);
            List<ProductToolResult> toolResults = Product.toToolResults(result);
            putCache(cacheKey, toolResults);
            return toolResults;
        }
        return new ArrayList<>();
    }

    @Tool(description = "【必须调用】根据多个条件搜索商品，支持多维度筛选。" +
            "当你需要推荐、对比、搜索商品时，必须先调用此工具获取真实商品数据，禁止编造商品信息。" +
            "搜索维度包括：" +
            "- keyword: 商品名称或关键词（支持模糊匹配）" +
            "- brand: 品牌名称（精确匹配）" +
            "- category: 商品分类（一级分类，精确匹配）" +
            "- subCategory: 二级分类（精确匹配）" +
            "- minPrice: 最低价格" +
            "- maxPrice: 最高价格" +
            "- status: 商品状态（1=上架，0=下架）" +
            "- sortBy: 排序字段（可选：price、created_at）" +
            "- sortOrder: 排序方向（可选：asc、desc，默认desc）" +
            "- limit: 返回数量限制（默认10，最大50）" +
            "返回字段说明：id(商品ID), productCode(商品编码), title(商品名称), brand(品牌), category(一级分类), subCategory(二级分类), basePrice(价格), salesCount(销量), status(1=上架/0=下架), mainImageUrl(主图URL)。" +
            "适用于：用户想查找特定商品、了解有哪些商品可选、比较不同商品时。")
    public List<ProductToolResult> searchProducts(
            @ToolParam(required = false, description = "商品名称或关键词，支持模糊搜索") String keyword,
            @ToolParam(required = false, description = "品牌名称，精确匹配") String brand,
            @ToolParam(required = false, description = "一级分类名称，精确匹配") String category,
            @ToolParam(required = false, description = "二级分类名称，精确匹配") String subCategory,
            @ToolParam(required = false, description = "最低价格筛选") BigDecimal minPrice,
            @ToolParam(required = false, description = "最高价格筛选") BigDecimal maxPrice,
            @ToolParam(required = false, description = "商品状态：1=上架，0=下架，默认查询上架商品") Integer status,
            @ToolParam(required = false, description = "排序字段：price(价格)、created_at(创建时间)") String sortBy,
            @ToolParam(required = false, description = "排序方向：asc(升序)、desc(降序)，默认降序") String sortOrder,
            @ToolParam(required = false, description = "返回结果数量限制，默认20，最大100") Integer limit) {

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(w -> w.like("title", keyword)
                    .or().like("brand", keyword)
                    .or().like("category", keyword)
                    .or().like("sub_category", keyword)
                    .or().like("product_code", keyword));
        }

        if (brand != null && !brand.trim().isEmpty()) {
            queryWrapper.eq("brand", brand);
        }

        if (category != null && !category.trim().isEmpty()) {
            queryWrapper.eq("category", category);
        }

        if (subCategory != null && !subCategory.trim().isEmpty()) {
            queryWrapper.eq("sub_category", subCategory);
        }

        if (minPrice != null) {
            queryWrapper.ge("base_price", minPrice);
        }

        if (maxPrice != null) {
            queryWrapper.le("base_price", maxPrice);
        }

        if (status != null) {
            queryWrapper.eq("status", status);
        } else {
            queryWrapper.eq("status", 1);
        }

        if (sortBy != null && !sortBy.trim().isEmpty()) {
            String order = "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
            switch (sortBy.toLowerCase()) {
                case "price":
                    queryWrapper.orderBy(true, "asc".equals(order), "base_price");
                    break;
                case "created_at":
                    queryWrapper.orderBy(true, "asc".equals(order), "created_at");
                    break;
                default:
                    queryWrapper.orderByDesc("base_price");
            }
        } else {
            queryWrapper.orderByDesc("base_price");
        }

        int resultLimit = limit != null && limit > 0 ? Math.min(limit, 50) : 10;
        queryWrapper.last("LIMIT " + resultLimit);

        return Product.toToolResults(productMapper.selectList(queryWrapper));
    }

    @Tool(description = "【图片识别推荐使用】根据品牌和/或分类组合搜索商品，一次调用即可同时按品牌+分类筛选。" +
            "支持品牌名模糊匹配，例如：识别出'苹果'可匹配到'Apple 苹果'，识别出'兰黛'可匹配到'雅诗兰黛'。" +
            "可同时传入brandKeyword+category+subCategory缩小范围，也可只传其中一个。" +
            "返回字段说明：id(商品ID), productCode(商品编码), title(商品名称), brand(品牌), category(一级分类), subCategory(二级分类), basePrice(价格), salesCount(销量), status(1=上架/0=下架), mainImageUrl(主图URL)。")
    public List<ProductToolResult> searchByBrandAndCategory(
            @ToolParam(required = false, description = "品牌名称关键词，支持模糊匹配，如'苹果'、'兰黛'、'耐克'") String brandKeyword,
            @ToolParam(required = false, description = "一级分类名称，精确匹配，如'美妆护肤'、'数码电子'") String category,
            @ToolParam(required = false, description = "二级分类名称，精确匹配，如'精华'、'智能手机'") String subCategory,
            @ToolParam(required = false, description = "最低价格筛选") BigDecimal minPrice,
            @ToolParam(required = false, description = "最高价格筛选") BigDecimal maxPrice,
            @ToolParam(required = false, description = "返回结果数量限制，默认20，最大100") Integer limit) {

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();

        if (brandKeyword != null && !brandKeyword.trim().isEmpty()) {
            queryWrapper.like("brand", brandKeyword);
        }

        if (category != null && !category.trim().isEmpty()) {
            queryWrapper.eq("category", category);
        }
        if (subCategory != null && !subCategory.trim().isEmpty()) {
            queryWrapper.eq("sub_category", subCategory);
        }
        if (minPrice != null) {
            queryWrapper.ge("base_price", minPrice);
        }
        if (maxPrice != null) {
            queryWrapper.le("base_price", maxPrice);
        }

        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("base_price");

        int resultLimit = limit != null && limit > 0 ? Math.min(limit, 50) : 10;
        queryWrapper.last("LIMIT " + resultLimit);

        return Product.toToolResults(productMapper.selectList(queryWrapper));
    }

    @Tool(description = "获取商品分类列表和统计信息，返回所有一级分类及每个分类下的商品数量。" +
            "适用于：了解平台有哪些商品类别、辅助商品推荐时的分类筛选。" +
            "注意：调用此工具后，如需具体商品数据，还需调用searchProducts工具。")
    public List<ProductToolResult> getCategories() {
        List<ProductToolResult> cached = getCache("categories");
        if (cached != null) {
            return cached;
        }
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("category, COUNT(*) as count")
                .eq("status", 1)
                .groupBy("category")
                .orderByDesc("count");
        List<ProductToolResult> result = Product.toToolResults(productMapper.selectList(queryWrapper));
        putCache("categories", result);
        return result;
    }

    @Tool(description = "【必须调用】获取热门商品列表，按销量排序返回热门商品。" +
            "当用户没有明确需求、想看热门推荐时，必须调用此工具获取真实热门商品数据。" +
            "返回字段说明：id(商品ID), productCode(商品编码), title(商品名称), brand(品牌), category(一级分类), subCategory(二级分类), basePrice(价格), salesCount(销量), status(1=上架/0=下架), mainImageUrl(主图URL)。" +
            "适用于：用户没有明确需求时的商品推荐、热门商品展示。")
    public List<ProductToolResult> getHotProducts(@ToolParam(required = false, description = "返回数量，默认10，最大50") Integer limit) {
        int resultLimit = limit != null && limit > 0 ? Math.min(limit, 50) : 10;
        String cacheKey = "hot:" + resultLimit;
        List<ProductToolResult> cached = getCache(cacheKey);
        if (cached != null) {
            return cached;
        }
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1)
                .orderByDesc("base_price")
                .last("LIMIT " + resultLimit);
        List<ProductToolResult> result = Product.toToolResults(productMapper.selectList(queryWrapper));
        putCache(cacheKey, result);
        return result;
    }
}