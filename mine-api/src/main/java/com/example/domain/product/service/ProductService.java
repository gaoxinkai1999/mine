package com.example.domain.product.service;

import com.example.domain.batch.entity.Batch;
import com.example.domain.batch.repository.BatchRepository;
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
import com.example.query.InventoryQuery;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private BatchRepository batchRepository; // 批次仓库

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
        // 新增：设置临期监控阈值
        product.setExpiryMonitoringThresholdDays(productDto.getExpiryMonitoringThresholdDays());
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
        QCategory category = QCategory.category; // 添加类别引用
        
        // 创建一个基本查询，不使用distinct
        JPAQuery<Product> jpaQuery = queryFactory.select(product)
                                                 .from(product)
                                                 .leftJoin(product.category, category);

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
        
        // 返回构建好的查询对象，使用groupBy避免重复结果
        return jpaQuery.where(where)
                       .groupBy(product.id) // 使用分组替代distinct
                       .orderBy(category.sort.asc(), product.sort.asc());
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
        
        // 如果需要加载类别，标记为fetchJoin
        if (query.getIncludes() != null && query.getIncludes().contains(ProductQuery.Include.CATEGORY)) {
            // 查找现有的category join并标记为fetchJoin
            // 注意: 在QueryDSL中这种操作可能不直接支持，我们的策略是确保在buildConditionQuery中已经建立了基本的join
            // 然后在这里，我们不再添加新的join，而是依赖已有的join
            // 下面这行代码可能会导致重复join，但通常Hibernate会处理这种情况
            jpaQuery.leftJoin(product.category).fetchJoin();
        }
    }



    /**
     * 获取所有在售商品信息，包含库存信息
     */
    public List<ProductDto> getProducts() {
        // 直接构建查询，避免使用BaseRepository.findList()可能引发的distinct问题
        QProduct product = QProduct.product;
        QCategory category = QCategory.category;
        
        // 创建查询，使用GROUP BY而非DISTINCT
        List<Product> products = queryFactory.select(product)
                                            .from(product)
                                            .leftJoin(product.category, category)
                                            .where(product.del.eq(false))
                                            .groupBy(product.id)
                                            .orderBy(category.sort.asc(), product.sort.asc())
                                            .fetch();
        
        // 获取产品库存信息
        List<ProductStockDTO> productStocks = inventoryService.getProductStocks(products.stream()
                                                                               .map(Product::getId)
                                                                               .toList());
        
        // 组装结果
        return products.stream()
                      .map(p -> {
                          // 映射基础信息
                          ProductDto productDto = productMapper.toProductDto(p);
                          // 设置库存信息
                          productDto.setProductStockDTO(productStocks.stream()
                                                                    .filter(stockInfo -> stockInfo.getProductId()
                                                                                                 .equals(p.getId()))
                                                                    .findFirst()
                                                                    .orElseThrow(() -> new MyException("商品库存信息不存在")));
                          return productDto;
                      })
                      .toList();
    }

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

    @Transactional
    public void convertToBatchProduct(Integer productId, LocalDate productionDate) {
        // 1. 验证商品
        Product product = productRepository.findById(productId)
                                           .orElseThrow(() -> new MyException("商品不存在: " + productId));
        if (product.isBatchManaged()) {
            throw new MyException("商品已经是批次管理商品: " + product.getName());
        }

        // 2. 查询现有非批次库存
        Optional<Inventory> existingInventoryOpt = inventoryService.findOne(
                InventoryQuery.builder().productId(productId).batchId(null).build() // 显式查找 batchId 为 null 的
        );

        Inventory existingInventory = existingInventoryOpt.orElse(null);
        int currentQuantity = (existingInventory != null) ? existingInventory.getQuantity() : 0;
        log.info("商品ID: {}, 名称: {}, 现有非批次库存数量: {}", productId, product.getName(), currentQuantity);

        Batch initialBatch = null; // 初始化为 null

        // 3. 如果有库存，创建初始批次并更新库存记录
        if (currentQuantity > 0) {
            if (existingInventory == null) {
                // 按理说，如果 quantity > 0，应该存在 inventory 记录，但为了健壮性处理
                throw new MyException("库存数据异常：存在数量但未找到对应的非批次库存记录");
            }
            // 3.1 创建初始批次
            initialBatch = new Batch();
            initialBatch.setProduct(product);
            initialBatch.setProductionDate(productionDate);
            // 处理批次号，自动生成
            String batchNum = batchService.generateBatchNumber(productionDate);
            initialBatch.setBatchNumber(batchNum);
            initialBatch.setCostPrice(product.getCostPrice());
            initialBatch.setStatus(true);
            initialBatch.setCreatedTime(LocalDateTime.now());
            // 注意：不设置 purchaseDetail，因为这是转换现有库存创建的初始批次

            initialBatch = batchRepository.save(initialBatch); // 保存批次获取ID
            log.info("商品ID: {}, 创建初始批次: ID={}, 批次号={}", productId, initialBatch.getId(), batchNum);

            // 3.2 更新库存记录以关联到新批次
            existingInventory.setBatch(initialBatch); // 关联批次
            inventoryRepository.save(existingInventory); // 保存更新
            log.info("商品ID: {}, 现有库存 {} 已关联到初始批次ID: {}", productId, currentQuantity, initialBatch.getId());

        } else {
            // 如果现有库存为0，检查是否仍存在旧的 batchId 为 NULL 的记录，如果存在可以考虑删除或保留
            if (existingInventory != null) {
                // 可选：删除数量为0的旧库存记录
                // inventoryRepository.delete(existingInventory);
                log.info("商品ID: {}, 现有非批次库存为0，保留原库存记录", productId);
            } else {
                log.info("商品ID: {}, 无现有非批次库存记录", productId);
            }
        }

        // 4. 更新商品为批次管理
        product.setBatchManaged(true);
        productRepository.save(product);

        log.info("商品ID: {} 已成功转换为批次管理商品。", productId);
    }

}
