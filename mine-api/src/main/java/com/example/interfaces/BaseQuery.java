package com.example.interfaces;

import java.util.Set;

// 1. 基础查询接口
public interface BaseQuery {
    /**
     * 获取需要加载的关联实体集合
     * @return 关联实体集合
     */
    Set<?> getIncludes();
}