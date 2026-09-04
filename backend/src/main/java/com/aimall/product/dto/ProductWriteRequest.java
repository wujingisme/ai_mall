package com.aimall.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** 后台商品创建/完整替换请求；imageUrl 和 description 为空表示清除。 */
public record ProductWriteRequest(
        @NotBlank(message = "SKU 不能为空") @Size(max = 64, message = "SKU 最多 64 个字符") String sku,
        @NotBlank(message = "商品名称不能为空") @Size(max = 200, message = "商品名称最多 200 个字符") String name,
        @NotNull(message = "价格不能为空") @DecimalMin(value = "0.00", message = "价格不能小于 0")
        @Digits(integer = 8, fraction = 2, message = "价格最多 8 位整数和 2 位小数") BigDecimal price,
        @NotNull(message = "库存不能为空") @Min(value = 0, message = "库存不能小于 0") Integer stock,
        @NotNull(message = "状态不能为空") @Min(value = 0, message = "状态只能为 0 或 1")
        @Max(value = 1, message = "状态只能为 0 或 1") Integer status,
        @Size(max = 1000, message = "图片地址最多 1000 个字符") String imageUrl,
        String description
) {}
