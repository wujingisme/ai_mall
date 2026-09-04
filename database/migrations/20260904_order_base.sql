-- AI Mall 订单第 1 阶段：订单基础和预览所需的数据表。
-- 本阶段只建立订单结构；真正创建订单、锁库存和生成取货码在后续迁移/阶段实现。
USE mall;

CREATE TABLE IF NOT EXISTS mall_order (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING_PICKUP',
  pickup_location_name VARCHAR(200) NOT NULL,
  pickup_location_address VARCHAR(500) NOT NULL,
  pickup_code_hash CHAR(64) NULL,
  total_amount DECIMAL(10, 2) UNSIGNED NOT NULL,
  item_quantity INT UNSIGNED NOT NULL,
  idempotency_key VARCHAR(64) NULL,
  cancelled_at DATETIME(3) NULL,
  picked_up_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_mall_order_order_no (order_no),
  UNIQUE KEY uk_mall_order_pickup_code_hash (pickup_code_hash),
  UNIQUE KEY uk_mall_order_user_idempotency (user_id, idempotency_key),
  KEY idx_mall_order_user_created (user_id, created_at),
  KEY idx_mall_order_status_created (status, created_at),
  CONSTRAINT fk_mall_order_user FOREIGN KEY (user_id) REFERENCES mall_user(id),
  CONSTRAINT chk_mall_order_status CHECK (status IN ('PENDING_PICKUP', 'PICKED_UP', 'CANCELLED')),
  CONSTRAINT chk_mall_order_amount CHECK (total_amount >= 0),
  CONSTRAINT chk_mall_order_item_quantity CHECK (item_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线上订单主表';

CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NULL,
  sku VARCHAR(64) NOT NULL,
  product_name VARCHAR(200) NOT NULL,
  unit_price DECIMAL(10, 2) UNSIGNED NOT NULL,
  quantity INT UNSIGNED NOT NULL,
  line_amount DECIMAL(10, 2) UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_order_item_order (order_id),
  KEY idx_order_item_product (product_id),
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES mall_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE SET NULL,
  CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
  CONSTRAINT chk_order_item_amount CHECK (line_amount = unit_price * quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单商品快照明细';
