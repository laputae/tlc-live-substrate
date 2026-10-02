CREATE TABLE IF NOT EXISTS t_redpacket_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  red_packet_id BIGINT NOT NULL,
  amount_fen INT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'WIN',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  archived TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_rp_user (red_packet_id, user_id)
) ENGINE=InnoDB;
