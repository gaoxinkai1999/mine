package com.example.domain.order.service;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQueryFactory;

public class UserService extends BaseService<User,Integer, QUser>{
    public UserService(JPAQueryFactory queryFactory, EntityPathBase qEntity, Class entityClass) {
        super(queryFactory, QUser.user, entityClass);
    }
}
