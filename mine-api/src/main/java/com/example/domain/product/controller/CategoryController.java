package com.example.domain.product.controller;

import com.example.domain.product.dto.CategoryRequestDto;
import com.example.domain.product.dto.CategoryUpdateDto;
import com.example.domain.product.entity.Category;
import com.example.domain.product.service.CategoryService;
import com.example.query.CategoryQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Tag(name = "category", description = "商品品类处理")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @Operation(summary = "查询所有品类",
            description = "")
    @GetMapping("/list")
    public List<Category> getCategories() {
        return categoryService.findList(CategoryQuery.builder().del(false)
                                                     .build());
    }
    @Operation(summary = "软删除品类", description = "通过品类ID进行软删除操作")
    @DeleteMapping("/{categoryId}")
    public void deleteCategory(@PathVariable Integer categoryId) {
        categoryService.deleteCategory(categoryId);
    }
    @Operation(summary = "新建品类",
            description = "")
    @PostMapping("/create")
    public void create(@RequestBody CategoryRequestDto categoryRequestDto) {
        categoryService.createCategory(categoryRequestDto);
    }

    @Operation(summary = "修改品类",
            description = "")
    @PostMapping("/update")
    public void update(@RequestBody List<CategoryRequestDto> categorys) {
        categoryService.update(categorys);
    }

    /**
     * 批量更新类别信息
     *
     * @param categories 类别更新请求列表
     */
    @Operation(summary = "批量更新类别信息")
    @PostMapping("/batch-update")
    public void batchUpdate(@RequestBody List<CategoryUpdateDto> categories) {
        categoryService.batchUpdate(categories);
    }

}
