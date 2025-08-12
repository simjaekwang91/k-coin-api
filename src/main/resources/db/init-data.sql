INSERT IGNORE INTO process_lock (lock_key)
VALUES ('global');

-- 1) 이벤트
INSERT IGNORE INTO `event` (id, event_name, starts_at, ends_at, created_at, updated_at)
VALUES (1, '2025 여름휴가 이벤트', '2025-08-01 00:00:00', '2025-08-31 23:59:59', NOW(), NOW());

-- 2) 보상
INSERT IGNORE INTO reward (id, event_id, reward_name, winning_quota, required_coins, created_at, updated_at)
VALUES (1, 1, '1일 휴가권', 3, 1, NOW(), NOW()),
       (2, 1, '3일 휴가권', 3, 2, NOW(), NOW());

-- 3) 코인
INSERT IGNORE INTO coin (id, event_id, per_user_limit, per_issue_count, total_coin_count, remain_coin_count, created_at,
                         updated_at)
VALUES (1, 1, 3, 1, 900, 893, NOW(), NOW());

-- 4) 유저 코인
INSERT IGNORE INTO user_coin (id, user_id, coin_id, balance, acquired_total, created_at, updated_at)
VALUES (1, '1001', 1, 1, 2, NOW(), NOW()),
       (2, '1002', 1, 2, 2, NOW(), NOW()),
       (3, '1003', 1, 0, 0, NOW(), NOW()),
       (4, '1004', 1, 1, 3, NOW(), NOW());

-- 5) 응모 엔트리
INSERT IGNORE INTO event_entry (id, user_id, reward_id, status, created_at, updated_at)
VALUES (1, '1001', 1, 'ENTERED', NOW(), NOW()),
       (2, '1002', 1, 'CANCELLED', NOW(), NOW()),
       (3, '1004', 2, 'ENTERED', NOW(), NOW());
