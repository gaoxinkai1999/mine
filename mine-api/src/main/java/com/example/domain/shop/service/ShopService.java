package com.example.domain.shop.service;


import com.example.domain.order.entity.QOrder;
import com.example.domain.product.entity.QProduct;
import com.example.domain.shop.dto.ShopArrearsDto;
import com.example.domain.shop.dto.ShopDto;
import com.example.domain.shop.dto.ShopRequestDto;
import com.example.domain.shop.entity.QShop;
import com.example.domain.shop.entity.Shop;
import com.example.domain.shop.mapper.ShopMapper;
import com.example.domain.shop.repository.ShopRepository;
import com.example.exception.MyException;
import com.example.interfaces.BaseRepository;
import com.example.query.ShopQuery;
import com.example.utils.ChinesePinyinFirstLetter;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商家管理服务
 * 处理商家的增删改查及相关操作
 */
@Service
@Slf4j
public class ShopService implements BaseRepository<Shop, ShopQuery> {

    @Autowired
    private ShopRepository shopRepository; // 商家仓库，用于与数据库交互

    @Autowired
    private ShopMapper shopMapper; // 商家映射器，用于对象转换

    @Autowired
    private JPAQueryFactory queryFactory; // JPA查询工厂

    private final QShop shop = QShop.shop; // 查询商家的QueryDSL对象

    private final QProduct product = QProduct.product;

    public void update(List<ShopRequestDto> shops) {
        // 更新商家信息
        for (ShopRequestDto shopRequestDto : shops) {
            Shop shop = shopRepository.findById(shopRequestDto.getId())
                                      .orElseThrow(() -> new MyException("商家未找到"));
            shopMapper.partialUpdate(shopRequestDto, shop);
            shopRepository.save(shop);
        }
    }

    /**
     * 获取所有店铺的欠款数据
     */
    public List<ShopArrearsDto> arrears() {
        ShopQuery build = ShopQuery.builder()
                                   .havaArrears(true)
                                   .del(false)
                                   .build();
        List<Shop> list = findList(build);

        return list.stream()
                   .map(shopMapper::toShopArrearsDto)
                   .collect(Collectors.toList());

    }


    /**
     * 按shop的     private char pinyin 字段分组，;
     */

    public Map<Character, List<Shop>> groupByPinyin(List<Shop> shops) {


        // 使用 Stream API 按 pinyin 字段分组
        return shops.stream()
                    .collect(Collectors.groupingBy(Shop::getPinyin));

    }


    @Override
    public JPAQuery<Shop> buildConditionQuery(ShopQuery query) {
        JPAQuery<Shop> jpaQuery = queryFactory
                .selectFrom(shop)
                .distinct();

        // 处理查询条件
        BooleanBuilder where = new BooleanBuilder();

        if (query.getId() != null) {
            where.and(shop.id.eq(query.getId()));
        }

        // 按名称或地址模糊查询
        if (query.getName() != null) {
            BooleanExpression nameOrLocation = shop.name.like("%" + query.getName() + "%")
                                                        .or(shop.location.like("%" + query.getName() + "%"));
            where.and(nameOrLocation);
        }
        // 是否查找经纬度不为空的
        if (query.getLocation() != null) {
            if (query.getLocation()) {
                where.and(shop.latitude.isNotNull()
                                       .and(shop.longitude.isNotNull()));
            } else {
                where.and(shop.latitude.isNull()
                                       .and(shop.longitude.isNull()));
            }
        }

        if (query.getPinyin() != null) {
            where.and(shop.pinyin.eq(query.getPinyin()));
        }

        if (query.getDel() != null) {
            where.and(shop.del.eq(query.getDel()));
        }

        if (query.getSlow() != null) {
            where.and(shop.slow.eq(query.getSlow()));
        }
        if (query.getHavaArrears() != null) {
            where.and(shop.arrears.gt(0));
        }

        return jpaQuery
                .where(where)
                .orderBy(shop.createTime.desc());
    }

    @Override
    public void buildRelationship(ShopQuery query, JPAQuery<Shop> jpaQuery) {
        // 已移除价格规则相关关联逻辑
    }

    /**
     * 新建店铺
     *
     * @param shop
     */
    public void create(ShopDto shop) {
        shop.setPinyin(ChinesePinyinFirstLetter.getFirstLetterOfFirstCharacter(shop.getName()));
        shop.setArrears(BigDecimal.ZERO);
        shop.setSlow(false);
        shop.setCreateTime(LocalDate.now());
        shop.setDel(false);
        Shop shopEntity = shopMapper.toEntity(shop);
        shopRepository.save(shopEntity);

    }

    // --- 使用 QueryDSL 实现的新查询方法 ---

    /**
     * 使用 QueryDSL 查询过去指定天数内活跃的、且未删除的商家列表 (无分页)。
     * “活跃”定义为在指定天数内有过订单记录。
     *
     * @param daysThreshold 活跃天数阈值 (例如 90)
     * @return 活跃商家实体列表 (List<Shop>)
     */
    public List<Shop> findActiveShopsQueryDSL(int daysThreshold) {
        QOrder order = QOrder.order; // 获取订单实体的 Q 类实例
        // 计算截止日期时间点（例如：90天前的 00:00:00）
        LocalDateTime cutoffDateTime = LocalDate.now()
                                                .minusDays(daysThreshold)
                                                .atStartOfDay();

        log.debug("使用 QueryDSL 查询近 {} 天活跃商家...", daysThreshold);


        List<Shop> activeShops = queryFactory
                .selectFrom(shop) // SELECT * FROM shop s
                .where(
                        shop.del.isFalse() // WHERE s.is_del = false
                                .and( // AND
                                        // 使用 EXISTS 子查询判断是否存在符合条件的订单
                                        JPAExpressions
                                                .selectOne() // SELECT 1
                                                .from(order) // FROM `order` o
                                                .where(
                                                        order.shop.id.eq(shop.id) // WHERE o.shop_id = s.id
                                                                     .and(order.createTime.goe(cutoffDateTime)) // AND o.create_time >= cutoffDateTime
                                                )
                                                .exists() // EXISTS (...)
                                )
                )
                // 与默认查询保持一致的排序
                .orderBy(shop.pinyin.asc(), shop.createTime.desc())
                .fetch(); // 执行查询并获取结果列表

        log.info("QueryDSL 查询到 {} 个近 {} 天活跃的商家。", activeShops.size(), daysThreshold);
        return activeShops;

    }

    /**
     * 使用 QueryDSL 查询在过去指定天数内不活跃的、但未删除的商家列表 (无分页)。
     * “不活跃”定义为在指定天数内没有任何订单记录。
     *
     * @param daysThreshold 不活跃天数阈值 (例如 90)
     * @return 不活跃商家实体列表 (List<Shop>)
     */

    public List<Shop> findInactiveShopsQueryDSL(int daysThreshold) {
        QOrder order = QOrder.order;
        LocalDateTime cutoffDateTime = LocalDate.now()
                                                .minusDays(daysThreshold)
                                                .atStartOfDay();

        log.debug("使用 QueryDSL 查询近 {} 天不活跃商家...", daysThreshold);

        List<Shop> inactiveShops = queryFactory
                .selectFrom(shop) // SELECT * FROM shop s
                .where(
                        shop.del.isFalse() // WHERE s.is_del = false
                                .and( // AND
                                        // 使用 NOT EXISTS 子查询判断是否不存在符合条件的订单
                                        JPAExpressions
                                                .selectOne() // SELECT 1
                                                .from(order) // FROM `order` o
                                                .where(
                                                        order.shop.id.eq(shop.id) // WHERE o.shop_id = s.id
                                                                     .and(order.createTime.goe(cutoffDateTime)) // AND o.create_time >= cutoffDateTime
                                                )
                                                .notExists() // NOT EXISTS (...)
                                )
                )
                // 与默认查询保持一致的排序
                .orderBy(shop.pinyin.asc(), shop.createTime.desc())
                .fetch(); // 执行查询

        log.info("QueryDSL 查询到 {} 个近 {} 天不活跃的商家。", inactiveShops.size(), daysThreshold);
        return inactiveShops;

    }

}
