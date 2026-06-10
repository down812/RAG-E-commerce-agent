package com.ecommerceserver.result;

import java.util.List;

/**
 * 分页结果封装类：包含总记录数、总页数、当前页码、每页大小、当前页数据
 */
public class PageResult<T> {
    private Long total;      // 总记录数
    private Long pages;      // 总页数
    private Integer pageNum; // 当前页码
    private Integer pageSize;// 每页大小
    private List<T> rows;    // 当前页数据列表

    // 全参数构造方法（用于业务代码中封装分页结果）
    public PageResult(Long total, Long pages, Integer pageNum, Integer pageSize, List<T> rows) {
        this.total = total;
        this.pages = pages;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.rows = rows;
    }

    // Getter 和 Setter 方法
    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getPages() {
        return pages;
    }

    public void setPages(Long pages) {
        this.pages = pages;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }





}