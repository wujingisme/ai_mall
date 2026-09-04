package com.aimall.order.mapper;

import com.aimall.order.entity.OrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 订单商品明细数据访问接口。 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
