package com.fitness.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private Long total;
    private Long page;
    private Long size;
    private List<T> records;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords());
        return result;
    }

    public static <T> PageResult<T> of(Long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(1L);
        result.setSize((long) records.size());
        result.setRecords(records);
        return result;
    }
}