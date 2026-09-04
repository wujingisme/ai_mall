package com.aimall.coupon.mapper;

import com.aimall.coupon.entity.CouponShare;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 分享凭证的数据访问接口。 */
@Mapper
public interface CouponShareMapper extends BaseMapper<CouponShare> {
    /**
     * 条件递增领取次数。
     *
     * <p>返回 1 表示本次请求抢到名额；返回 0 表示分享已停用、过期或达到最大领取次数。
     * “检查并递增”放在一条 SQL 中，是并发安全的关键。</p>
     */
    @Update("UPDATE coupon_share SET claimed_count = claimed_count + 1 WHERE id=#{id} AND status='ACTIVE' AND expires_at>CURRENT_TIMESTAMP(3) AND claimed_count<max_claims")
    int consumeClaim(@Param("id") Long id);
}
