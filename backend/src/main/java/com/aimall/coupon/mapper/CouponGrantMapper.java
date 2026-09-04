package com.aimall.coupon.mapper;

import com.aimall.coupon.entity.CouponGrant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 人工发券审计记录的数据访问接口。 */
public interface CouponGrantMapper extends BaseMapper<CouponGrant> {}
