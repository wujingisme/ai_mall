package com.aimall.order.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 后台订单详情。
 *
 * <p>商品名称、SKU、单价和数量来自订单明细快照，而不是当前商品表，
 * 这样商品后续改名或下架也不会改变历史订单展示。取货码只允许在核销接口中
 * 由后台提交验证，详情响应永远不返回取货码明文或哈希。</p>
 */
public record AdminOrderDetailResponse(
        String id,
        String orderNo,
        String userId,
        String username,
        String displayName,
        String status,
        String pickupLocationName,
        String pickupLocationAddress,
        int itemQuantity,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime pickedUpAt) {
}
