# Resilient-Reward-Core 🏆

> **Java 21 Virtual Threads와 Spring Batch를 활용한 보상 및 알림 처리 시스템**

본 프로젝트는 서비스 초기 운영 환경에서 발생하는 대규모 트래픽 대응과 데이터 정합성 보장을 목표로 설계되었습니다. Node.js 기반의 비동기 스트림 처리 아키텍처를 Java 21 및 Spring Boot 환경에서 구현하여 시스템의 안정성과 확장성을 확보했습니다.

---

## 🚀 Key Technical Decisions

### 1. 비동기 I/O 최적화 (Java 21 Virtual Threads)
- **Background**: Node.js의 비동기 모델을 통해 확보했던 I/O 효율을 Java 환경에서 구현하고자 했습니다.
- **Decision**: **Java 21 가상 스레드(Virtual Threads)**를 도입하여 컨텍스트 스위칭 비용을 최소화하고, 수만 건의 알림 발송과 같은 I/O 바운드 작업에서 높은 동시성을 확보했습니다.
- **Implementation**: `ExecutorService`와 가상 스레드를 결합하여 대규모 푸시 알림 발송 엔진을 구축했습니다.

### 2. 메모리 효율적 대용량 데이터 처리 (Spring Batch)
- **Problem**: 수십만 건의 CSV 당첨자 데이터를 한 번에 메모리에 로드할 경우 OOM(Out Of Memory) 장애 위험이 있었습니다.
- **Solution**: **Spring Batch의 Chunk-oriented Processing** 패턴을 적용하여 데이터를 일정 단위로 나누어 처리하고, `JdbcTemplate` 벌크 인서트를 통해 데이터베이스 쓰기 성능을 최적화했습니다.
- **Result**: 데이터 규모와 무관하게 고정된 메모리 사용량으로 안정적인 운영이 가능해졌습니다.

### 3. 무결성 보장을 위한 락 전략 (JPA Pessimistic Lock & Redis)
- **Problem**: 한정된 수량의 보상을 지급할 때 발생하는 레이스 컨디션(Race Condition)으로 인한 중복 지급 문제.
- **Solution**:
    - **Redis Lua Script**: 진입 단계에서 Sliding Window Rate Limiting을 적용하여 비정상적 트래픽을 선제 차단합니다.
    - **MySQL Pessimistic Lock**: 보상 수량 차감 시 `SELECT ... FOR UPDATE`를 활용해 DB 수준의 원자성을 보장합니다.
- **Result**: 0.1%의 오차도 허용하지 않는 철저한 데이터 정합성을 달성했습니다.

---

## 🏗️ System Architecture

### 1. 실시간 보상 경로 (Real-time Path)
`User Request` → `Redis Rate Limiter` → `Weighted Random Engine` → `JPA Pessimistic Lock (Reward Deduction)` → `Virtual Thread Notification`

### 2. 운영 지원 경로 (Admin/Batch Path)
`CSV Upload` → `Spring Batch Reader` → `Reward Processing` → `Bulk Insert` → `Event-Driven Notification`

---

## 🛠️ Tech Stacks
- **Language**: Java 21
- **Framework**: Spring Boot 3.4.2, Spring Batch
- **Data**: Spring Data JPA (MySQL), Spring Data Redis
- **Resilience**: Resilience4j (RateLimiter, Bulkhead)
- **Build**: Gradle

---

## 📝 How to Run
```bash
./gradlew bootRun
```
*(Redis와 MySQL이 로컬에 실행 중이어야 합니다. 개발 환경에서는 H2와 내장 Redis 설정을 지원합니다.)*
