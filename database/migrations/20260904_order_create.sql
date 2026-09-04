-- AI Mall 订单第 2 阶段第二小步：正式创建订单所需的幂等请求摘要。
--
-- idempotency_key 只说明“这是哪一次客户端请求”；摘要用于判断网络重试时请求内容是否一致。
-- 订单创建代码会在同一事务中锁定购物车和商品库存，之后再写入订单与明细。
USE mall;

ALTER TABLE mall_order
  ADD COLUMN idempotency_payload_hash CHAR(64) NULL
    COMMENT '幂等键对应请求商品摘要，防止同键不同参数' AFTER idempotency_key;
