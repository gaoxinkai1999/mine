package com.example.domain.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "category")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Category implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // 类别名称
    @Column(nullable = false)
    private String name;


    // 排序号
    private int sort;


    // 创建时间
    @Column(nullable = false)
    private LocalDateTime createTime=LocalDateTime.now();


    @NotNull
    @ColumnDefault("0")
    @Column(name = "is_del", nullable = false)
    private boolean del;

}
