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
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0 下架，1 上架',
  image_url VARCHAR(1000) NULL COMMENT '主图 URL',
  description TEXT NULL COMMENT '商品描述',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_sku (sku),
  KEY idx_product_status_created (status, created_at),
  CONSTRAINT chk_product_status CHECK (status IN (0, 1))
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
