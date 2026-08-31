USE mall;
CREATE TABLE coupon_share (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, template_id BIGINT UNSIGNED NOT NULL,
  creator_user_id BIGINT UNSIGNED NOT NULL, creator_user_coupon_id BIGINT UNSIGNED NOT NULL,
  token_hash CHAR(64) NOT NULL, max_claims INT UNSIGNED NOT NULL DEFAULT 1,
  claimed_count INT UNSIGNED NOT NULL DEFAULT 0, status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  expires_at DATETIME(3) NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id), UNIQUE KEY uk_coupon_share_token_hash (token_hash),
  KEY idx_coupon_share_creator_created (creator_user_id, created_at), KEY idx_coupon_share_creator_coupon (creator_user_coupon_id),
  CONSTRAINT fk_coupon_share_template FOREIGN KEY (template_id) REFERENCES coupon_template(id),
  CONSTRAINT fk_coupon_share_creator FOREIGN KEY (creator_user_id) REFERENCES mall_user(id),
  CONSTRAINT chk_coupon_share_quantity CHECK (max_claims > 0 AND claimed_count <= max_claims),
  CONSTRAINT chk_coupon_share_status CHECK (status IN ('ACTIVE', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
ALTER TABLE user_coupon DROP FOREIGN KEY fk_user_coupon_grant;
ALTER TABLE user_coupon DROP CHECK chk_user_coupon_source;
ALTER TABLE user_coupon MODIFY grant_id BIGINT UNSIGNED NULL, ADD COLUMN share_id BIGINT UNSIGNED NULL AFTER grant_id;
ALTER TABLE user_coupon ADD KEY idx_user_coupon_share (share_id),
  ADD CONSTRAINT fk_user_coupon_grant FOREIGN KEY (grant_id) REFERENCES coupon_grant(id),
  ADD CONSTRAINT fk_user_coupon_share FOREIGN KEY (share_id) REFERENCES coupon_share(id),
  ADD CONSTRAINT chk_user_coupon_source CHECK ((source = 'MANUAL' AND grant_id IS NOT NULL AND share_id IS NULL) OR (source = 'SHARE' AND grant_id IS NULL AND share_id IS NOT NULL));
CREATE TABLE coupon_claim (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, share_id BIGINT UNSIGNED NOT NULL,
  claimant_user_id BIGINT UNSIGNED NOT NULL, user_coupon_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY (id),
  UNIQUE KEY uk_coupon_claim_share_user (share_id, claimant_user_id), UNIQUE KEY uk_coupon_claim_user_coupon (user_coupon_id),
  CONSTRAINT fk_coupon_claim_share FOREIGN KEY (share_id) REFERENCES coupon_share(id),
  CONSTRAINT fk_coupon_claim_user FOREIGN KEY (claimant_user_id) REFERENCES mall_user(id),
  CONSTRAINT fk_coupon_claim_coupon FOREIGN KEY (user_coupon_id) REFERENCES user_coupon(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
