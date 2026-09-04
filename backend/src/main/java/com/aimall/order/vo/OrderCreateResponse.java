package com.aimall.order.vo;

/**
 * 正式创建订单响应。
 *
 * <p>pickupCode 只在首次创建成功的响应中返回；订单详情接口只返回订单快照，
 * 不会重复返回取货码。replayed=true 表示这是同一幂等请求的重试结果。</p>
 */
public record OrderCreateResponse(
        OrderDetailResponse order,
        String pickupCode,
        boolean replayed) {
}
