package com.aimall.order.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 后台订单列表中的一行摘要。
 *
 * <p>后台需要知道订单属于哪个客户，但仍然只返回展示所需的用户名/显示名称，
 * 不返回密码摘要、微信 OpenID 或取货码哈希等内部字段。ID 使用字符串序列化，
 * 避免 JavaScript Number 处理较大的数据库主键时发生精度丢失。</p>
 */
public record AdminOrderSummaryResponse(
        String id,
        String orderNo,
        String userId,
        String username,
        String displayName,
        String status,
        String pickupLocationName,
        int itemQuantity,
        BigDecimal totalAmount,
        OffsetDateTime createdAt) {
}
