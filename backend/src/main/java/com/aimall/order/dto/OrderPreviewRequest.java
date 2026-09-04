package com.aimall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 订单预览请求；至少选择一件购物车商品，最多一次预览 50 种商品。 */
public record OrderPreviewRequest(
        @NotEmpty(message = "请至少选择一件商品")
        @Size(max = 50, message = "一次最多选择 50 种商品")
        List<@Valid OrderPreviewItemRequest> items) {
}
