# k-coin-api

코인발급 및 휴가 응모 api

행사(Event)-리워드(Reward)-코인(Coin) 발급/응모/취소를 지원하는 백엔드 서비스입니다.
사용자는 코인을 발급받아 리워드(휴가권)에 응모하고, 취소 시 코인 환불이 이뤄집니다

### Tech Stack ###

	Language: Kotlin (JDK 21)
	•	Framework: Spring Boot 3.5.4 (Spring MVC, Spring Data JPA, Springdoc OpenAPI)
	•	Build: Gradle (Wrapper 포함)
	•	RDBMS: MySQL
	•	테스트: Testcontainers(MySQL) / 단위 테스트 H2

### 프로젝트 구조 ###

    com.jasim.kcoinapi
    ├─ coin           # 코인 발급/조회 도메인
    ├─ event          # 리워드 응모/취소/조회 도메인
    ├─ common         # 공통락(ProcessLock), Emums, 공통 응답, Audit
    ├─ config         # 프로젝트 공통 설정
    ├─ exception      # 예외 처리, 에러 로그
    └─ integration    # 통합 테스트 (Testcontainers)

### 테이블 및 관계 ###

    모든 테이블은 논리적인 관계만 있고 물리적 관계는 없습니다.
	•	event(이벤트) 1 ── N reward(리워드(휴가권))
	•	event(이벤트) 1 ── N coin(이벤트에 발급된 코인)
	•	coin(코인) 1 ── N user_coin(사용자에게 발급된 코인 정보)
	•	reward(이벤트의 리워드(1,3일 휴가권)) 1 ── N event_entry (리워드 응모 이력)
	•	coin_log: 코인 로그(지급 ISSUE / 응모 ENTERED / 응모 취소 CANCEL)
	•	process_lock: 분산락(행 단위)용 테이블

### DDL / Seed Data ###

    기본 이벤트/리워드/코인 기본값이 들어 있습니다.
    유저 코인 정보/응모이력/코인로그는 샘플 데이터를 포함하지 않았습니다.(테스트 용이 위함)
    테스트 컨테이너를 활용한 통합 테스트에는 모든 샘플데이터를 포함하고 있습니다.
	•	DDL: src/main/resources/db/coin-schema.sql
	•	Seed: src/main/resources/db/init-data.sql

### 애플리케이션 실행 방법 ###

사전 요구사항

- JDK 21
- docker

    Docker MySQL 실행
    docker run --name kcoin-mysql -p 3306:3306 -e MYSQL_DATABASE=kcoin_db \
    -e MYSQL_USER=kcoin -e MYSQL_PASSWORD=kcoinpass \
    -e MYSQL_ROOT_PASSWORD=root -d mysql:8.0

API Documentation
- http://localhost:8080/swagger-ui/index.html
- 엔드포인트
    - 코인 발급
        - POST /v1/coins/issue-coin
            - Request Body
                - {
                  "eventId": 1,
                  "coinId": 1,
                  "userId": "jksim"
                  }
            - Response
                - { "status": "OK", "data": true }
    - 전체 코인 현황
        - GET /v1/coins/summary/{coinId} (예시 coinId = 1)
            - Response
                - {
                  "status": "OK",
                  "data": {
                  "remainCoinCount": 900,
                  "userCoinInfo": [
                  { "userId": "jksim", "acquiredTotal": 2, "balance": 1 },
                  ...
                  ]
                  }
                  }
    - 사용자 코인 현황
      - GET /v1/coins/summary/{coinId}/{userId} (예시 coinId = 1, userId = jksim)
        - Response
          - {
            "status": "OK",
            "data": { "userId":"jksim", "acquiredTotal":2, "balance":1 }
            }
    - 리워드 응모/취소
      - POST /v1/event/entry-reward/{eventId}/{rewardId}/{userId}?status=ENTERED|CANCELLED
        - Response
          - { "status":"OK", "data": true }
    - 리워드별(휴가권 별) 응모 현황
      - GET /v1/event/reward-entry-summary/{eventId}/{rewardId} (예시 eventId = 1, rewardId = 1)
        - Response
          - { "status":"OK", "data": { "rewardName":"1일 휴가권", "entryCount":1 } }
    - 사용자별 응모 현황
      - GET /v1/event/user-reward-entry-summary/{eventId}/{rewardId}/{userId}
      - (예시 eventId = 1, rewardId = 1, userId: "jksim")
        - Response
          - {
            "status":"OK",
            "data":{
            "userId":"1001",
            "entries":[
            {
            "eventName":"2025 여름휴가 이벤트",
            "rewardName":"1일 휴가권",
            "status":"ENTERED",
            "createTime":"2025-08-01T00:00:00Z",
            "updateTime":"2025-08-01T00:00:00Z"
            }
            ]
            }
            }
### Race Condition ###
    •	process_lock 테이블의 행 락을 이용해 임계구역을 보호하고 모든 요청이 순차 처리되도록 합니다.
	•	서비스 메서드 시작 시 lockRepository.lockWithTimeout(lockKey)로 
            전역 락 취득 → 코인 발급/응모/취소 중 과발급/이중처리 방지처리를 하였습니다.
	•	트랜잭션 타임아웃 및 예외 처리로 교착·장기 대기를 완화 하였습니다.

### 회고 ###
    오랜만에 순수 개발에 몰입할 수 있어 즐거웠습니다. 일정에 쫓기기보다 요구사항을 명확히 하고 구현에 집중하는 흐름을 다시 점검할 수 있었습니다.
    다음에 비슷한 과제를 한다면 RDBMS 제약이 없다는 가정 아래 Redis 중심으로 재구성해 보고 싶습니다. 
    전역 락 대신 분산 락, 또는 Lua 스크립트로 DECR을 원자적으로 수행한 뒤 검증하는 방식으로 바꾸면 
    대규모 트래픽에서도 과발급/중복 처리를 더 안전하게 막을 수 있다고 생각합니다.
    
