package com.aimall.cart.mapper;

import com.aimall.cart.entity.CartItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 购物车条目的数据访问接口。 */
public interface CartItemMapper extends BaseMapper<CartItem> {
}
