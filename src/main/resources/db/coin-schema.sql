CREATE TABLE IF NOT EXISTS event (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       event_name VARCHAR(50) NOT NULL,
                       starts_at DATETIME NOT NULL,
                       ends_at DATETIME NOT NULL,
                       created_at DATETIME NOT NULL,
                       updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS event_entry (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             user_id BIGINT NOT NULL,
                             reward_id BIGINT NOT NULL,
                             status ENUM('ENTERED','CANCELLED') NOT NULL DEFAULT 'ENTERED',
                             created_at DATETIME NOT NULL,
                             updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reward (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        event_id BIGINT NOT NULL,
                        reward_name VARCHAR(50) NOT NULL,
                        winning_quota INT NOT NULL,
                        required_coins INT NOT NULL,
                        created_at DATETIME NOT NULL,
                        updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coin (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      event_id BIGINT NOT NULL,
                      per_user_limit INT NOT NULL,
                      total_coin_count INT NOT NULL,
                      remain_coin_count INT NOT NULL,
                      created_at DATETIME NOT NULL,
                      updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_coin (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           user_id VARCHAR(20) NOT NULL,
                           coin_id BIGINT NOT NULL,
                           balance INT NOT NULL,
                           acquired_total INT NOT NULL,
                           created_at DATETIME NOT NULL,
                           updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coin_log (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          coin_id BIGINT NOT NULL,
                          amount INT NOT NULL,
                          reason VARCHAR(20) NOT NULL,              -- 'ACQUIRE', 'ENTER', 'CANCEL'
                          event_entry_id BIGINT NULL,
                          created_at DATETIME NOT NULL,
                          updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

