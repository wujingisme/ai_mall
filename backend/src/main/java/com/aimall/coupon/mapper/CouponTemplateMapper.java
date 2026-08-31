package com.aimall.coupon.mapper;

import com.aimall.coupon.entity.CouponTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
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

}
