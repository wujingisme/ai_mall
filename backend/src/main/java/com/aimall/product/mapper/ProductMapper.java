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
    /** 在创建订单事务中锁住商品行，按商品 ID 升序获取锁以降低死锁概率。 */
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
}
