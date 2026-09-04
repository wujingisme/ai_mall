CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE mall;

CREATE TABLE IF NOT EXISTS mall_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  username VARCHAR(64) NOT NULL COMMENT '登录名',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
  display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
  avatar_url VARCHAR(1000) NULL,
  wechat_open_id VARCHAR(64) NULL COMMENT '微信小程序 OpenID，同一小程序内唯一',
  wechat_union_id VARCHAR(64) NULL COMMENT '微信开放平台 UnionID，可用于跨应用合并身份',
  roles VARCHAR(200) NOT NULL DEFAULT 'CUSTOMER' COMMENT '逗号分隔角色：CUSTOMER、OPERATOR、ADMIN、SUPER_ADMIN',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  failed_login_attempts INT UNSIGNED NOT NULL DEFAULT 0,
  locked_until DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_mall_user_username (username),
  UNIQUE KEY uk_mall_user_wechat_open_id (wechat_open_id),
  KEY idx_mall_user_wechat_union_id (wechat_union_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商城用户';

-- 首次部署时需要由运维手动把一个可信账号提升为超级管理员，之后才能在后台创建其他管理账号。
-- UPDATE mall_user SET roles = 'SUPER_ADMIN' WHERE username = '你的超级管理员用户名';

CREATE TABLE IF NOT EXISTS auth_session (
  id CHAR(36) NOT NULL COMMENT '会话 ID',
  user_id BIGINT UNSIGNED NOT NULL,
  refresh_token_hash CHAR(64) NOT NULL COMMENT 'SHA-256 摘要',
  expires_at DATETIME(3) NOT NULL,
  revoked_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_session_token_hash (refresh_token_hash),
  KEY idx_auth_session_user (user_id),
  CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES mall_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证会话';

CREATE TABLE IF NOT EXISTS product (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品 ID',
  sku VARCHAR(64) NOT NULL COMMENT '商品编码',
  name VARCHAR(200) NOT NULL COMMENT '商品名称',
  price DECIMAL(10, 2) UNSIGNED NOT NULL COMMENT '商品价格',
  stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '库存',
  reserved_stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已被未完成订单预留的库存',
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0 下架，1 上架',
  image_url VARCHAR(1000) NULL COMMENT '主图 URL',
  description TEXT NULL COMMENT '商品描述',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_sku (sku),
  KEY idx_product_status_created (status, created_at),
  CONSTRAINT chk_product_status CHECK (status IN (0, 1)),
  CONSTRAINT chk_product_reserved_stock CHECK (reserved_stock <= stock)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品';

CREATE TABLE IF NOT EXISTS cart_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  product_id BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
  quantity INT UNSIGNED NOT NULL COMMENT '购买数量',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_cart_item_user_product (user_id, product_id),
  KEY idx_cart_item_product (product_id),
  CONSTRAINT fk_cart_item_user FOREIGN KEY (user_id) REFERENCES mall_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
  CONSTRAINT chk_cart_item_quantity CHECK (quantity BETWEEN 1 AND 99)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户购物车商品';

CREATE TABLE IF NOT EXISTS coupon_template (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '优惠券模板 ID',
  name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
  coupon_type VARCHAR(32) NOT NULL COMMENT '首版仅支持 FIXED_AMOUNT',
  minimum_spend DECIMAL(10, 2) UNSIGNED NOT NULL COMMENT '使用门槛金额',
  discount_amount DECIMAL(10, 2) UNSIGNED NOT NULL COMMENT '优惠金额',
  total_quantity INT UNSIGNED NOT NULL COMMENT '总发行量',
  issued_quantity INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已发行数量',
  per_user_limit INT UNSIGNED NOT NULL COMMENT '每人限领数量',
  validity_type VARCHAR(32) NOT NULL COMMENT 'FIXED_RANGE 或 DAYS_AFTER_RECEIPT',
  valid_from DATETIME(3) NULL COMMENT '固定有效期开始时间',
  valid_until DATETIME(3) NULL COMMENT '固定有效期结束时间',
  valid_days INT UNSIGNED NULL COMMENT '领取后有效天数',
  share_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许分享领取',
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT、ACTIVE、DISABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_coupon_template_status_created (status, created_at),
  CONSTRAINT chk_coupon_template_type CHECK (coupon_type = 'FIXED_AMOUNT'),
  CONSTRAINT chk_coupon_template_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED')),
  CONSTRAINT chk_coupon_template_amount CHECK (minimum_spend > 0 AND discount_amount > 0 AND discount_amount < minimum_spend),
  CONSTRAINT chk_coupon_template_quantity CHECK (total_quantity > 0 AND issued_quantity <= total_quantity AND per_user_limit BETWEEN 1 AND total_quantity),
  CONSTRAINT chk_coupon_template_validity CHECK (
    (validity_type = 'FIXED_RANGE' AND valid_from IS NOT NULL AND valid_until IS NOT NULL AND valid_until > valid_from AND valid_days IS NULL)
    OR
    (validity_type = 'DAYS_AFTER_RECEIPT' AND valid_from IS NULL AND valid_until IS NULL AND valid_days > 0)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS coupon_grant (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '人工发放记录 ID',
  template_id BIGINT UNSIGNED NOT NULL,
  target_user_id BIGINT UNSIGNED NOT NULL,
  operator_user_id BIGINT UNSIGNED NOT NULL,
  requested_quantity INT UNSIGNED NOT NULL,
  success_quantity INT UNSIGNED NOT NULL,
  reason VARCHAR(200) NOT NULL,
  idempotency_key VARCHAR(64) NOT NULL COMMENT '客户端生成的幂等键',
  status VARCHAR(16) NOT NULL COMMENT '首版为 SUCCESS',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_coupon_grant_idempotency (idempotency_key),
  KEY idx_coupon_grant_target_created (target_user_id, created_at),
  KEY idx_coupon_grant_template_created (template_id, created_at),
  CONSTRAINT fk_coupon_grant_template FOREIGN KEY (template_id) REFERENCES coupon_template(id),
  CONSTRAINT fk_coupon_grant_target FOREIGN KEY (target_user_id) REFERENCES mall_user(id),
  CONSTRAINT fk_coupon_grant_operator FOREIGN KEY (operator_user_id) REFERENCES mall_user(id),
  CONSTRAINT chk_coupon_grant_quantity CHECK (requested_quantity > 0 AND success_quantity BETWEEN 0 AND requested_quantity),
  CONSTRAINT chk_coupon_grant_status CHECK (status = 'SUCCESS')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券人工发放审计记录';

CREATE TABLE IF NOT EXISTS coupon_share (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_id BIGINT UNSIGNED NOT NULL,
  creator_user_id BIGINT UNSIGNED NOT NULL,
  creator_user_coupon_id BIGINT UNSIGNED NOT NULL,
  token_hash CHAR(64) NOT NULL,
  max_claims INT UNSIGNED NOT NULL DEFAULT 1,
  claimed_count INT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  expires_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id), UNIQUE KEY uk_coupon_share_token_hash (token_hash),
  KEY idx_coupon_share_creator_created (creator_user_id, created_at),
  KEY idx_coupon_share_creator_coupon (creator_user_coupon_id),
  CONSTRAINT fk_coupon_share_template FOREIGN KEY (template_id) REFERENCES coupon_template(id),
  CONSTRAINT fk_coupon_share_creator FOREIGN KEY (creator_user_id) REFERENCES mall_user(id),
  CONSTRAINT chk_coupon_share_quantity CHECK (max_claims > 0 AND claimed_count <= max_claims),
  CONSTRAINT chk_coupon_share_status CHECK (status IN ('ACTIVE', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券分享凭证';

CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户优惠券实例 ID',
  user_id BIGINT UNSIGNED NOT NULL,
  template_id BIGINT UNSIGNED NOT NULL,
  grant_id BIGINT UNSIGNED NULL,
  share_id BIGINT UNSIGNED NULL,
  source VARCHAR(16) NOT NULL COMMENT 'MANUAL 或 SHARE',
  name VARCHAR(100) NOT NULL COMMENT '发放时的模板名称快照',
  coupon_type VARCHAR(32) NOT NULL,
  minimum_spend DECIMAL(10, 2) UNSIGNED NOT NULL,
  discount_amount DECIMAL(10, 2) UNSIGNED NOT NULL,
  valid_from DATETIME(3) NOT NULL,
  valid_until DATETIME(3) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED、USED；EXPIRED 由有效期实时派生',
  used_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_user_coupon_user_status_validity (user_id, status, valid_until),
  KEY idx_user_coupon_template_user (template_id, user_id),
  KEY idx_user_coupon_grant (grant_id),
  KEY idx_user_coupon_share (share_id),
  CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES mall_user(id),
  CONSTRAINT fk_user_coupon_template FOREIGN KEY (template_id) REFERENCES coupon_template(id),
  CONSTRAINT fk_user_coupon_grant FOREIGN KEY (grant_id) REFERENCES coupon_grant(id),
  CONSTRAINT fk_user_coupon_share FOREIGN KEY (share_id) REFERENCES coupon_share(id),
  CONSTRAINT chk_user_coupon_source CHECK ((source = 'MANUAL' AND grant_id IS NOT NULL AND share_id IS NULL) OR (source = 'SHARE' AND grant_id IS NULL AND share_id IS NOT NULL)),
  CONSTRAINT chk_user_coupon_type CHECK (coupon_type = 'FIXED_AMOUNT'),
  CONSTRAINT chk_user_coupon_amount CHECK (minimum_spend > 0 AND discount_amount > 0 AND discount_amount < minimum_spend),
  CONSTRAINT chk_user_coupon_validity CHECK (valid_until > valid_from),
  CONSTRAINT chk_user_coupon_status CHECK (status IN ('UNUSED', 'USED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户优惠券实例';

CREATE TABLE IF NOT EXISTS coupon_claim (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  share_id BIGINT UNSIGNED NOT NULL,
  claimant_user_id BIGINT UNSIGNED NOT NULL,
  user_coupon_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id), UNIQUE KEY uk_coupon_claim_share_user (share_id, claimant_user_id),
  UNIQUE KEY uk_coupon_claim_user_coupon (user_coupon_id),
  CONSTRAINT fk_coupon_claim_share FOREIGN KEY (share_id) REFERENCES coupon_share(id),
  CONSTRAINT fk_coupon_claim_user FOREIGN KEY (claimant_user_id) REFERENCES mall_user(id),
  CONSTRAINT fk_coupon_claim_coupon FOREIGN KEY (user_coupon_id) REFERENCES user_coupon(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券分享领取记录';

-- 订单基础表：第一版线上下单、线下取货，不包含物流和真实支付字段。
CREATE TABLE IF NOT EXISTS mall_order (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单数据库主键',
  order_no VARCHAR(40) NOT NULL COMMENT '对外展示的业务订单号',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '下单用户',
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING_PICKUP' COMMENT 'PENDING_PICKUP、PICKED_UP、CANCELLED',
  pickup_location_name VARCHAR(200) NOT NULL COMMENT '取货点名称快照',
  pickup_location_address VARCHAR(500) NOT NULL COMMENT '取货点地址快照',
  pickup_code_hash CHAR(64) NULL COMMENT '取货码 SHA-256 摘要，第二阶段启用',
  total_amount DECIMAL(10, 2) UNSIGNED NOT NULL COMMENT '后端计算的订单总金额',
  item_quantity INT UNSIGNED NOT NULL COMMENT '订单商品总件数',
  idempotency_key VARCHAR(64) NULL COMMENT '客户端创建订单幂等键，第二阶段启用',
  idempotency_payload_hash CHAR(64) NULL COMMENT '幂等键对应请求商品摘要，防止同键不同参数',
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

-- 订单明细保存下单时的商品快照，商品后续改名、调价或删除不影响历史订单展示。
CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_id BIGINT UNSIGNED NOT NULL COMMENT '所属订单',
  product_id BIGINT UNSIGNED NULL COMMENT '原商品 ID，商品删除后可为空',
  sku VARCHAR(64) NOT NULL COMMENT '下单时 SKU 快照',
  product_name VARCHAR(200) NOT NULL COMMENT '下单时商品名称快照',
  unit_price DECIMAL(10, 2) UNSIGNED NOT NULL COMMENT '下单时单价快照',
  quantity INT UNSIGNED NOT NULL COMMENT '购买数量',
  line_amount DECIMAL(10, 2) UNSIGNED NOT NULL COMMENT '商品行金额快照',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_order_item_order (order_id),
  KEY idx_order_item_product (product_id),
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES mall_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE SET NULL,
  CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
  CONSTRAINT chk_order_item_amount CHECK (line_amount = unit_price * quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单商品快照明细';
