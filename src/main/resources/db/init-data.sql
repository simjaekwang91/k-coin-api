-- 1) 이벤트 초기 값
INSERT IGNORE INTO event (
    id, event_name, starts_at, ends_at, created_at, updated_at
) VALUES
    (1, '2025 여름휴가 이벤트',
     '2025-08-01 00:00:00',
     '2025-08-31 23:59:59',
     NOW(), NOW());

-- 2) 보상 초기
INSERT IGNORE INTO reward (
    id, event_id, reward_name, winning_quota, required_coins, created_at, updated_at
) VALUES
      (1, 1, '1일 휴가권', 3, 1, NOW(), NOW()),
      (2, 1, '3일 휴가권', 2, 2, NOW(), NOW());

-- 3) 코인 초기값
INSERT IGNORE INTO coin (
    id, event_id, per_user_limit, per_issue_count, total_coin_count, remain_coin_count, created_at, updated_at
) VALUES
    (1, 1, 3, 1, 900, 900, NOW(), NOW());

INSERT IGNORE INTO process_lock(lock_key) VALUES ('global');

