package com.example.domain.returnOrder.entity;

import com.example.domain.shop.entity.Shop;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "return_order", schema = "mine")
public class ReturnOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "create_time")
    private Instant createTime;

    @Column(name = "amount", precision = 5, scale = 2)
    private BigDecimal amount;

    @OneToMany(mappedBy = "returnOrder")
    private List<ReturnOrderDetail> returnOrderDetails = new ArrayList<>();

}