package com.aimall.coupon.mapper;

import com.aimall.coupon.entity.CouponTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

@Mapper
/** 优惠券模板数据库访问接口；除了通用 CRUD，还包含防超发的原子更新。 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {
    // 条件更新把状态、有效期和剩余发行量校验收敛到一条 SQL，避免并发请求超发。
    @Update("""
            UPDATE coupon_template
            SET issued_quantity = issued_quantity + #{quantity}, updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{templateId}
              AND status = 'ACTIVE'
              AND issued_quantity + #{quantity} <= total_quantity
              AND (validity_type <> 'FIXED_RANGE' OR valid_until > CURRENT_TIMESTAMP(3))
            """)
    int reserveIssueQuantity(@Param("templateId") Long templateId, @Param("quantity") int quantity);

    @Select("SELECT * FROM coupon_template WHERE id = #{id} FOR UPDATE")
    /** 在事务中锁住模板行，让领取流程按模板串行判断库存和用户限领。 */
    CouponTemplate selectByIdForUpdate(@Param("id") Long id);

}
