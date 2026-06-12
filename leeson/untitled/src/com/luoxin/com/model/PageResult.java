package com.luoxin.com.model;

import java.util.List;

/**
 * 分页查询结果
 */
public class PageResult {

    private List<Student> list;
    private int page;
    private int pageSize;
    private int totalCount;
    private int totalPages;

    public List<Student> getList() {
        return list;
    }

    public void setList(List<Student> list) {
        this.list = list;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
