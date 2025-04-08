package com.example.query;

import com.example.interfaces.BaseQuery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryQuery implements BaseQuery {
    private Integer id;
    private String name;
    private Boolean del;
    private Set<Include> includes = new HashSet<>();

    public enum Include {

    }

}