package com.aimall.order.mapper;

import com.aimall.order.entity.MallOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 订单主表数据访问接口；分页和基础 CRUD 由 MyBatis-Plus 提供。 */
@Mapper
public interface MallOrderMapper extends BaseMapper<MallOrder> {
}
