package com.example.domain.product.service;

import com.example.domain.batch.service.BatchService;
import com.example.domain.inventory.entity.Inventory;
import com.example.domain.inventory.repository.InventoryRepository;
import com.example.domain.inventory.service.InventoryService;
import com.example.domain.product.dto.ProductDto;
import com.example.domain.product.dto.ProductStockDTO;
import com.example.domain.product.dto.ProductUpdateDto;
import com.example.domain.product.entity.Product;
import com.example.domain.product.entity.QCategory;
import com.example.domain.product.entity.QProduct;
import com.example.domain.product.mapper.ProductMapper;
import com.example.domain.product.repository.ProductRepository;
import com.example.domain.shop.service.ShopService;
import com.example.exception.MyException;
import com.example.interfaces.BaseRepository;
import com.example.query.CategoryQuery;
import com.example.query.ProductQuery;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 产品管理服务
 * 处理商品的增删改查及相关操作
 */
@Slf4j
@Service
public class   ProductService implements BaseRepository<Product, ProductQuery> {
    @Autowired
    private ProductRepository productRepository; // 产品仓库，用于与数据库交互
    @Autowired
    private JPAQueryFactory queryFactory; // JPA查询工厂
    @Autowired
    private CategoryService categoryService;
    @Lazy
    @Autowired
    private InventoryService inventoryService; // 库存服务

    @Autowired
    private BatchService batchService; // 批次服务


    @Autowired
    private ProductMapper productMapper; // 产品映射器，用于对象转换
    @Autowired
    private ShopService shopService;
    
    @Autowired
    private InventoryRepository inventoryRepository;


    /**软删除商品 */
    public void deleteProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new MyException("商品不存在: " + productId));
        product.setDel(true);
        productRepository.save(product);
    }
    /**
     * 新建商品
     * @param productDto 商品DTO
     * @return 新创建的商品ID
     * @throws MyException 如果商品名称已存在或价格无效
     */
    @Transactional
    public int createProduct(ProductDto productDto) {
        // 检查商品名称是否已存在
        if (productRepository.existsByName(productDto.getName())) {
            throw new MyException("商品名称已存在: " + productDto.getName());
        }
        
        // 验证价格
        if (productDto.getCostPrice().compareTo(BigDecimal.ZERO) < 0 || 
            productDto.getDefaultSalePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new MyException("价格不能为负数");
        }

        // 创建商品
        Product product = new Product();
        product.setBatchManaged(productDto.isBatchManaged());
        product.setCategory(categoryService.findOne(CategoryQuery.builder()
                .id(productDto.getCategoryId())
                .build())
                .orElseThrow(() -> new MyException("类别不存在: " + productDto.getCategoryId())));
        product.setName(productDto.getName());
        product.setCostPrice(productDto.getCostPrice());
        product.setDefaultSalePrice(productDto.getDefaultSalePrice());
        product.setDel(false);
        product.setSort(productRepository.findMaxSort() + 1);
        product = productRepository.save(product);
        
        // 初始化库存记录
        Inventory inventory = inventoryService.findOrCreateInventory(product, null);
        inventory.setQuantity(0);
        inventoryRepository.save(inventory);
        
        log.info("创建商品成功, ID: {}, 已初始化库存", product.getId());
        return product.getId();
    }

    /**
     * 批量更新产品信息
     *
     * @param products 产品更新请求列表
     */
    @Transactional
    public void batchUpdate(List<ProductUpdateDto> products) {
        for (ProductUpdateDto productUpdateDto : products) {
            Product product = this.findOne(ProductQuery.builder()
                    .id(productUpdateDto.getId())
                    .includes(Set.of(ProductQuery.Include.CATEGORY))
                    .build())
                    .orElseThrow(() -> new MyException("产品不存在: " + productUpdateDto.getId()));

            Product update = productMapper.partialUpdate(productUpdateDto, product);
            productRepository.save(update);
        }
    }

    /**
     * 构建基本条件查询方法
     * 根据传入的查询对象构建一个包含基本条件的JPA查询
     *
     * @param query 查询条件对象，包含了一系列查询条件
     * @return 返回一个包含基本条件的JPAQuery对象
     */
    @Override
    public JPAQuery<Product> buildConditionQuery(ProductQuery query) {
        QProduct product = QProduct.product; // 查询产品的QueryDSL对象
        
        // 创建一个选择所有字段的查询，并确保结果唯一
        JPAQuery<Product> jpaQuery = queryFactory.selectFrom(product)
                                                 .distinct();

        // 初始化查询条件构建器
        BooleanBuilder where = new BooleanBuilder();

        // 处理查询条件
        // 根据产品ID进行查询
        if (query.getId() != null) {
            where.and(product.id.eq(query.getId()));
        }

        // 根据产品名称进行模糊查询
        if (query.getName() != null) {
            where.and(product.name.like("%" + query.getName() + "%"));
        }

        // 根据类别ID进行查询
        if (query.getCategoryId() != null) {
            where.and(product.category.id.eq(query.getCategoryId()));
        }

        // 根据是否删除进行查询
        if (query.getDel() != null) {
            where.and(product.del.eq(query.getDel()));
        }
        if (query.getBatchManaged() != null) {
            where.and(product.batchManaged.eq(query.getBatchManaged()));
        }
        if (query.getIds() != null) {
            where.and(product.id.in(query.getIds()));
        }

        // 返回构建好的查询对象，应用查询条件和排序
        return jpaQuery.where(where)
                       .orderBy(product.category.sort.asc(), product.sort.asc());
    }

    /**
     * 构建关联加载方法
     * 根据传入的查询对象为JPAQuery添加关联查询
     *
     * @param query 查询条件对象，包含了需要加载的关联
     * @param jpaQuery 已构建的基本查询对象
     */
    @Override
    public void buildRelationship(ProductQuery query, JPAQuery<Product> jpaQuery) {
        QProduct product = QProduct.product; // 查询产品的QueryDSL对象
        QCategory category = QCategory.category; // 查询类别的QueryDSL对象
        
        // 默认加载类别
        jpaQuery.leftJoin(product.category, category)
                .fetchJoin();
                
        // 这里可以添加更多关联加载逻辑，如果有需要的话
    }



    /**
     * 获取所有在售商品信息，包含库存信息
     */

    public List<ProductDto> getProducts() {
        ProductQuery build = ProductQuery.builder()
                                         .del(false)
                                         .includes(Set.of(ProductQuery.Include.CATEGORY))
                                         .build();
        List<Product> products = this.findList(build);
        List<ProductStockDTO> productStocks = inventoryService.getProductStocks(products.stream()
                                                                                        .map(Product::getId)
                                                                                        .toList());
        return products.stream()
                       .map(product -> {
                           // 映射基础信息
                           ProductDto productDto = productMapper.toProductDto(product);
                           // 设置库存信息
                           productDto.setProductStockDTO(productStocks.stream()
                                                                      .filter(stockInfo -> stockInfo.getProductId()
                                                                                                    .equals(product.getId()))
                                                                      .findFirst()
                                                                      .orElseThrow(() -> new MyException("商品库存信息不存在")));
                           return productDto;
                       })
                       .toList();

    }


    // /**
    //  * 将非批次商品转换为批次商品
    //  *
    //  * @param productId 商品ID
    //  * @return 转换后的商品
    //  */
    // @Transactional
    // public Product convertToBatchProduct(Integer productId) {
    //     Product product = this.findOne(ProductQuery.builder()
    //                                                .id(productId)
    //                                                .build())
    //                           .orElseThrow(() -> new MyException("商品不存在: " + productId));
    //     if (product.isBatchManaged()) {
    //         throw new MyException("商品已经是批次管理商品: " + product.getName());
    //     }
    //
    //     // 商品设置为批次管理
    //     product.setBatchManaged(true);
    //     productRepository.save(product);
    //
    //     // 创建默认批次（可选）
    //     Batch defaultBatch = new Batch();
    //     defaultBatch.setProduct(product);
    //     defaultBatch.setBatchNumber("DEFAULT_BATCH"); // 这里可以根据需求生成批次号
    //     defaultBatch.setStatus(true);
    //     // 其他必要的字段可以设置为默认值
    //     Batch batch = batchService.saveBatch(defaultBatch);
    //
    //     // 检查并初始化库存记录
    //     Inventory inventory = inventoryService.findOrCreateInventory(product, null);
    //     InventoryUpdateDto inventoryUpdateDto = new InventoryUpdateDto();
    //     inventoryUpdateDto.setId(inventory.getId());
    //     inventoryUpdateDto.setProductId(productId);
    //     inventoryUpdateDto.setBatchId(batch.getId());
    //     inventoryService.batchUpdate(List.of(inventoryUpdateDto));
    //
    //     return product;
    // }
    //
    // /**
    //  * 将批次商品转换为非批次商品
    //  *
    //  * @param productId 商品ID
    //  * @return 转换后的商品
    //  */
    // @Transactional
    // public Product convertToNonBatchProduct(Integer productId) {
    //     Product product = this.findOne(ProductQuery.builder()
    //                                                .id(productId)
    //                                                .build())
    //                           .orElseThrow(() -> new MyException("商品不存在: " + productId));
    //
    //     if (!product.isBatchManaged()) {
    //         throw new MyException("商品已经是非批次管理商品: " + product.getName());
    //     }
    //
    //     // 设置为非批次管理
    //     product.setBatchManaged(false);
    //     productRepository.save(product);
    //
    //     // 删除所有相关批次（可选）
    //     List<Batch> batches = batchService.findByProduct(productId, true);
    //     for (Batch batch : batches) {
    //         batchService.deleteBatch(batch.getId());
    //     }
    //
    //     // 更新库存记录，确保库存与非批次管理状态一致
    //     Inventory inventory = inventoryService.findOrCreateInventory(product, null);
    //     inventoryService.updateInventory(product.getId(), -inventory.getQuantity(), OperationType.销售出库, null); // Adjust inventory
    //
    //     return product;
    // }

    /**
     * 根据商品ID获取商品最早销售日期
     *
     * @param productId 商品ID
     * @return 商品最早销售日期
     */
    public LocalDate getEarliestSaleDateByProductId(Integer productId) {
        // 调用ProductRepository的方法获取最早销售日期
        return productRepository.findEarliestSaleDateByProductId(productId);
    }

}
