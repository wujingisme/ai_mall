-- AI Mall 订单第 2 阶段第一小步：为商品增加“预留库存”。
--
-- stock 表示仓库中尚未完成销售的总库存；reserved_stock 表示已被待取货订单占用的数量。
-- 因此当前真正可再次下单的数量是 stock - reserved_stock。
-- 这份迁移只增加字段和数据库约束，不会创建订单、不扣减库存，也不会删除已有数据。
USE mall;

ALTER TABLE product
  ADD COLUMN reserved_stock INT UNSIGNED NOT NULL DEFAULT 0
    COMMENT '已被未完成订单预留的库存' AFTER stock,
  ADD CONSTRAINT chk_product_reserved_stock
    CHECK (reserved_stock <= stock);
