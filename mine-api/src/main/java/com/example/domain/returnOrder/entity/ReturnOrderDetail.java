package com.example.domain.returnOrder.entity;

import com.example.domain.batch.entity.Batch; // Import Batch entity
import com.example.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "return_order_detail", schema = "mine")
public class ReturnOrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_order_id")
    private ReturnOrder returnOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Lob
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ReturnType type;
    
    @Column(name = "quantity")
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY) // Add ManyToOne relationship to Batch
    @JoinColumn(name = "batch_id") // Assuming a column named batch_id in the database
    private Batch batch;

}