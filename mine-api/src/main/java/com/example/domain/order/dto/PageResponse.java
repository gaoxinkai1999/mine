package com.example.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.List;

/**
 * 分页响应DTO
 * 用于封装分页查询结果
 *
 * @param <T> 内容类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    // 当前页内容
    private List<T> content;
    
    // 是否有下一页
    private boolean hasNext;
    
    // 当前页码
    private int pageNumber;
    
    // 每页大小
    private int pageSize;
    
    /**
     * 从Spring Data Slice创建分页响应
     *
     * @param slice Spring Data分页结果
     * @param <T> 内容类型
     * @return 分页响应DTO
     */
    public static <T> PageResponse<T> fromSlice(Slice<T> slice) {
        return new PageResponse<>(
                slice.getContent(),
                slice.hasNext(),
                slice.getNumber(),
                slice.getSize()
        );
    }
} 