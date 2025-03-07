package com.example.domain.returnOrder.dto;

import com.example.domain.shop.dto.ShopSimpleDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 退货订单数据传输对象
 */
@Data
public class ReturnOrderDto {
    /**
     * 退货订单ID
     */
    private Integer id;
    
    /**
     * 商店信息
     */
    private ShopSimpleDto shop;
    
    /**
     * 创建时间
     */
    private Instant createTime;
    
    /**
     * 退货金额
     */
    private BigDecimal amount;
    
    /**
     * 退货订单详情列表
     */
    private List<ReturnOrderDetailDto> returnOrderDetails = new ArrayList<>();
} 