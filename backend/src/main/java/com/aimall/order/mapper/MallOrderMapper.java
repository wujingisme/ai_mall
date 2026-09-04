package com.aimall.order.mapper;

import com.aimall.order.entity.MallOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/** 订单主表数据访问接口；分页和基础 CRUD 由 MyBatis-Plus 提供。 */
@Mapper
public interface MallOrderMapper extends BaseMapper<MallOrder> {
    /**
     * 在取消订单事务中锁住当前用户自己的订单行。
     *
     * <p>用户 ID 同时放进查询条件，既保证数据归属，又避免把“查到订单后再判断用户”
     * 变成容易遗漏的两步逻辑。FOR UPDATE 会让取消和未来的后台核销串行处理。</p>
     */
    @Select("SELECT * FROM mall_order WHERE id = #{orderId} AND user_id = #{userId} FOR UPDATE")
    MallOrder selectForUpdate(@Param("userId") Long userId, @Param("orderId") Long orderId);

    /**
     * 只允许把待取货订单改成已取消。
     *
     * <p>即使 Service 前面已经锁住订单，旧状态条件仍然保留，作为数据库层的第二道并发保护；
     * 返回 1 才表示状态切换成功，0 表示订单已经被其他流程改变。</p>
     */
    @Update("UPDATE mall_order SET status = 'CANCELLED', cancelled_at = #{cancelledAt}, "
            + "updated_at = #{updatedAt} WHERE id = #{orderId} AND user_id = #{userId} "
            + "AND status = 'PENDING_PICKUP'")
    int cancelPending(@Param("userId") Long userId,
                      @Param("orderId") Long orderId,
                      @Param("cancelledAt") LocalDateTime cancelledAt,
                      @Param("updatedAt") LocalDateTime updatedAt);
}
