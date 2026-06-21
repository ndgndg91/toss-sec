# Toss DCA Trader (토스증권 DCA 보완형 자동매매 시스템)

토스증권 공식 Open API를 연동하여 현재 보유 중인 모든 종목을 자동으로 감지하고, 당일 하락률에 비례하여 매수 가중치를 적용하여 평균 단가를 효율적으로 낮추는 기계적 하락장 분할 매수(DCA) 엔진임.

---

## 🛠️ 기술 스택 및 환경
- **Language**: Kotlin 2.4.0 (K2 Compiler)
- **Runtime**: JDK 25 (LTS)
- **Framework**: Spring Boot 4.1.0
- **Database**: PostgreSQL 17 (Docker Container)
- **Concurrency**: Virtual Threads (`spring.threads.virtual.enabled=true`)
- **HTTP Client**: RestClient (with JdkClientHttpRequestFactory)

---

## ✨ 핵심 기능 상세

1. **보유 주식 자동 감지 (Multi-Stock Loop)**
   - `application.yml`에 종목 코드를 하드코딩하지 않고, 계좌 보유 주식 조회 API를 호출하여 현재 투자 중인 모든 종목을 대상으로 루프를 돌며 자동으로 DCA를 수행함.

2. **하락장 가중치 매수 공식 (DcaStrategy)**
   - 전일 종가 대비 당일 현재가 하락률에 비례하여 동적으로 매수 금액을 조율함.
     - **보합 ~ -3% 미만 하락**: 기본 DCA 금액 매수 (1.0배)
     - **-3% 이하 ~ -5% 미만 하락**: 기본 DCA 금액의 1.5배 매수
     - **-5% 이하 하락**: 기본 DCA 금액의 2.0배 매수

3. **안전장치 (Safety Capping & Dry-Run)**
   - **Max Budget Capping**: 예상 매수 금액이 설정된 일일 한도(`max-daily-budget`)를 초과할 경우, 최대 한도로 자동 캡핑하여 과도한 예수금 소모를 원천 차단함.
   - **Dry-Run Mode**: `dry-run: true` 설정 시 실제 주문 API 호출을 생략하고, 시뮬레이션 결과만 RDBMS에 기록하여 시스템 안전성을 사전 검증함.

4. **트레이서빌리티 (Traceability)**
   - 모든 API 통신 단계에 고유 `traceId`(UUID)를 강제 주입하여 로그 추적 및 거래 정합성 대사를 보장함.

5. **주문 이력 영속성 보장**
   - API 호출 직전 `PENDING` 상태로 DB에 이력을 선 저장한 후, 주문 성공/실패 여부에 따라 `SUCCESS`/`FAIL` 상태를 갱신함으로써 주문 유실 및 장애 추적이 용이함.

---

## 📂 프로젝트 구조 (DDD 4계층)

```text
src/main/kotlin/com/giri/trader/
├── TossDcaTraderApplication.kt (메인 실행 클래스)
├── domain/ (도메인 레이어 - 순수 비즈니스 규칙 및 연산 VO)
│   ├── Money.kt (금융 정밀 연산 VO)
│   ├── DcaStrategy.kt (가중치 계산 전략)
│   └── OrderHistory.kt (주문 이력 Entity)
├── application/ (애플리케이션 레이어 - 서비스 조율)
│   └── DcaTradingService.kt (DCA 전체 워크플로우 제어)
├── infrastructure/ (인프라 레이어 - 기술적 구현체 및 외부 API 연동)
│   ├── persistence/
│   │   └── OrderHistoryRepository.kt (RDBMS JPA 리포지토리)
│   └── toss/
│       ├── TossApiConfig.kt (RestClient 타임아웃 및 커넥션 풀 설정)
│       ├── TossApiClient.kt (토스증권 시세, 계좌, 주문 API 구현체)
│       ├── TossTokenManager.kt (OAuth 토큰 발급 및 메모리 캐싱)
│       └── Dto/ (API 바인딩 및 요청/응답 객체군)
└── interfaces/ (인터페이스 레이어 - 외부 엔트리 포인트)
    └── scheduler/
        └── DcaScheduler.kt (크론 기반 스케줄 트리거)
```

---

## 🚀 로컬 실행 방법

### 1. 로컬 데이터베이스(PostgreSQL) 구동
```bash
docker-compose up -d
```
*데이터는 프로젝트 루트 내부의 `db-data/` 디렉토리에 영속화되며, 깃 허브 업로드 시 자동으로 제외됩니다.*

### 2. 비트워든(Bitwarden) API 키 및 환경변수 주입
```bash
# 1. Bitwarden 세션 해제 및 세션 주입
export BW_SESSION=$(bw unlock --raw)

# 2. API Key 조회하여 환경변수 등록
export TOSS_SEC_API_KEY=$(bw get notes toss_sec_api_key)
export TOSS_SEC_SECRET_KEY=$(bw get notes toss_sec_secret_key)
```

### 3. 애플리케이션 실행
```bash
./gradlew bootRun
```
