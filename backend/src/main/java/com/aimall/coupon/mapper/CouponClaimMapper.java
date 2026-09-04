package com.aimall.coupon.mapper;

import com.aimall.coupon.entity.CouponClaim;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 分享领取审计记录的数据访问接口；唯一索引保证同一用户不能重复领取同一分享。 */
@Mapper
public interface CouponClaimMapper extends BaseMapper<CouponClaim> {
}
