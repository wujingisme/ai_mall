package com.aimall.product.mapper;

import com.aimall.product.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 商品表的数据访问接口，通用分页/增删改查由 MyBatis-Plus 提供。 */
public interface ProductMapper extends BaseMapper<Product> {
}
