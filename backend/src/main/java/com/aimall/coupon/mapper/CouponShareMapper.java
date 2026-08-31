package com.aimall.coupon.mapper;
import com.aimall.coupon.entity.CouponShare; import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*;
@Mapper public interface CouponShareMapper extends BaseMapper<CouponShare> {
 @Update("UPDATE coupon_share SET claimed_count = claimed_count + 1 WHERE id=#{id} AND status='ACTIVE' AND expires_at>CURRENT_TIMESTAMP(3) AND claimed_count<max_claims") int consumeClaim(@Param("id") Long id);
}
