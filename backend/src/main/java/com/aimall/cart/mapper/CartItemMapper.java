package com.aimall.cart.mapper;

import com.aimall.cart.entity.CartItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
/** 购物车条目的数据访问接口。 */
public interface CartItemMapper extends BaseMapper<CartItem> {
    /**
     * 在创建订单事务中锁住当前用户的购物车行。
     *
     * <p>锁住后，其他请求不能同时修改或删除这条购物车记录，保证校验数量、
     * 删除已下单条目和订单明细使用的是同一份数据。</p>
     */
    @Select("SELECT * FROM cart_item WHERE user_id = #{userId} AND product_id = #{productId} FOR UPDATE")
    CartItem selectForUpdate(@Param("userId") Long userId, @Param("productId") Long productId);
}
