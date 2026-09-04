package com.aimall.common.exception;

/**
 * 后台修改商品总库存时触发的库存约束冲突。
 *
 * <p>订单预留库存属于已经承诺给顾客的数量，不能因为管理员把总库存改小而被覆盖。
 * Service 抛出这个异常后，由全局异常处理器转换成稳定的 409 错误码。</p>
 */
public class ProductStockConflictException extends RuntimeException {
    public ProductStockConflictException(Long productId, int reservedStock) {
        super("商品库存不能低于已预留数量：productId=" + productId + ", reservedStock=" + reservedStock);
    }
}
