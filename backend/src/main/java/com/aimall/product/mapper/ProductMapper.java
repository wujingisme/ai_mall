package com.aimall.product.mapper;

import com.aimall.product.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
/** 商品表的数据访问接口，通用分页/增删改查由 MyBatis-Plus 提供。 */
public interface ProductMapper extends BaseMapper<Product> {
    /** 在创建订单或后台删除前锁住商品行，按商品 ID 升序获取锁以降低并发竞态和死锁概率。 */
    @Select("SELECT * FROM product WHERE id = #{productId} FOR UPDATE")
    Product selectForUpdate(@Param("productId") Long productId);

    /**
     * 条件增加订单预留库存。
     *
     * <p>返回 1 才表示本次成功占用库存；返回 0 代表商品下架、可售库存不足或数据不满足约束。
     * quantity 来自 DTO 校验后的整数，不拼接进 SQL，避免注入风险。</p>
     */
    @Update("UPDATE product SET reserved_stock = reserved_stock + #{quantity} "
            + "WHERE id = #{productId} AND status = 1 "
            + "AND reserved_stock <= stock "
            + "AND stock - reserved_stock >= #{quantity}")
    int reserveStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 释放取消订单占用的预留库存。
     *
     * <p>商品可能已经下架，但只要商品行还存在就必须允许释放；reserved_stock >= quantity
     * 条件用于阻止重复释放把库存减成负数。调用方在同一事务中先取得商品行锁。</p>
     */
    @Update("UPDATE product SET reserved_stock = reserved_stock - #{quantity} "
            + "WHERE id = #{productId} AND reserved_stock >= #{quantity}")
    int releaseReservedStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
