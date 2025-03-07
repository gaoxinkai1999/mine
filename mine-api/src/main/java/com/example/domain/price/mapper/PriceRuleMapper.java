package com.example.domain.price.mapper;

import com.example.domain.price.dto.PriceRuleDto;
import com.example.domain.price.dto.PriceRuleSimpleDto;
import com.example.domain.price.entity.PriceRule;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PriceRuleMapper {


    PriceRuleDto toPriceRuleDto(PriceRule priceRule);


    PriceRuleSimpleDto toPriceRuleSimpleDto(PriceRule priceRule);

    PriceRule toEntity(PriceRuleDto priceRuleDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    PriceRule partialUpdate(PriceRuleDto priceRuleDto, @MappingTarget PriceRule priceRule);
}