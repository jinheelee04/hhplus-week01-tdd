# 💰 포인트 관리 시스템 (TDD 실습)

> **TDD 기반 포인트 관리 서비스**  
> 포인트 충전·사용·조회 기능을 테스트 주도 개발 방식으로 구현하고,  
> `ReentrantLock` 기반 사용자 단위 **동시성 제어**를 적용했습니다.

![Java](https://img.shields.io/badge/Java-17-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen?logo=springboot)
![JUnit5](https://img.shields.io/badge/Test-JUnit5-red?logo=junit5)
![Gradle](https://img.shields.io/badge/Build-Gradle-blue?logo=gradle)

---

## 💡 개발 개요
이 프로젝트는 `Test-Driven Development`의 실무 적용 및 **동시성 안전한 포인트 관리**를 목표로 개발되었습니다.

테스트를 먼저 작성하고, 기능을 점진적으로 확장하며  
비즈니스 정책 검증, 예외 처리, Lock 기반 동시성 제어까지 단계적으로 완성했습니다.

---

## 🧩 주요 기능

| 기능 | 설명 |
|------|------|
| **포인트 조회** | 사용자의 현재 포인트 잔액 조회 |
| **포인트 충전** | 포인트 충전 (최소 100원, 최대 50,000원 / 회, 일일 한도 100,000원) |
| **포인트 사용** | 포인트 차감 (최소 100원, 최대 50,000원 / 회) |
| **포인트 내역 조회** | 충전 및 사용 이력 확인 |

---

## 🧪 TDD 개발 프로세스

본 프로젝트는 **Red → Green → Refactor**의 TDD 사이클로 진행되었습니다.

1. **Red** – 실패하는 테스트 작성
   > 예: “100원 미만 충전 시 실패해야 한다”
2. **Green** – 최소한의 코드로 테스트 통과
3. **Refactor** – 중복 제거 및 구조 개선
---
## ⚙️ 예외 처리 정책

비즈니스 정책 위반 시 명시적으로 `IllegalArgumentException` 또는 `IllegalStateException`을 발생시키며,  
`ApiControllerAdvice`에서 HTTP 400 응답으로 변환합니다.

| 정책 위반 상황 | 예외 타입 | 상태 코드 | 메시지 예시                              |
|----------------|------------|------------|-------------------------------------|
| 최소 충전 금액 미만 | `IllegalArgumentException` | 400 | "충전 금액은 최소 100원 이상이어야 합니다."         |
| 최대 충전 금액 초과 | `IllegalArgumentException` | 400 | "1회 최대 충전 금액은 50,000원입니다." |
| 최소 사용 금액 미만 | `IllegalArgumentException` | 400 | "사용 금액은 최소 100원 이상이어야 합니다." |
| 최대 사용 금액 초과 | `IllegalArgumentException` | 400 | "1회 최대 사용 금액은 50,000원입니다." |
| 일일 충전 한도 초과 | `IllegalStateException` | 400 | "일일 충전 한도(100,000원)을 초과했습니다."       |
| 최대 잔액 초과 | `IllegalStateException` | 400 | "최대 잔액은 100,000원을 초과할 수 없습니다."      |
| 잔액 부족 | `IllegalStateException` | 400 | "포인트 잔액이 부족합니다. (현재 잔액: X원, 사용 시도: Y원)" |

---

## 🔒 동시성 제어 방식

### ✅ 적용 방식: `ReentrantLock + ConcurrentHashMap`

- 사용자별 `ReentrantLock`을 `ConcurrentHashMap`에 저장하여 관리
- 동일 사용자 요청은 직렬화, 서로 다른 사용자는 병렬 처리 가능
- `finally` 블록에서 Lock을 해제하여 예외 발생 시에도 안전하게 처리

### 💡 특징
- **사용자별 Lock 분리** → 같은 유저의 요청만 순차적으로 처리
- **병렬 처리 가능** → 다른 유저 간 요청은 동시에 처리 가능
- **안전한 해제 보장** → 예외 발생 시에도 Lock은 반드시 해제됨

### ⚠️ 단점 및 개선 방안

| 항목 | 문제점 | 개선 방안 |
|------|--------|-----------|
| **분산 환경 미지원** | JVM 단위 Lock이라 서버 확장 시 동기화 불가 | Redis 분산 Lock 또는 DB Lock으로 확장 |
| **메모리 누수 가능성** | Lock 객체가 Map에 계속 쌓임 | WeakHashMap 사용 또는 TTL 기반 정리 |
| **데드락 위험** | 여러 Lock을 동시에 획득할 경우 | Lock 순서 일관성 유지로 예방 |

---

## 🧾 테스트 검증 항목

| 테스트 시나리오 | 검증 내용 |
|-----------------|-----------|
| 단일 사용자 동시 충전 | 여러 스레드가 동시에 충전해도 최종 잔액이 정확함 |
| 여러 사용자 병렬 충전 | 서로 다른 사용자 요청이 병렬로 처리됨 |
| 충전 + 사용 동시 요청 | 충전과 사용이 동시에 발생해도 포인트 정합성 유지 |
| 일일 한도 검증 | 다중 요청 상황에서도 일일 한도가 정확히 적용됨 |

---
## 🧰 기술 스택

| 구분 | 기술 |
|------|------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2.0 |
| **Build Tool** | Gradle |
| **Test** | JUnit 5, Mockito, MockMvc |
| **Concurrency** | ReentrantLock, ConcurrentHashMap |
---
## 📡 API 명세

| 메서드 | 엔드포인트 | 설명 |
|--------|-------------|------|
| `GET` | `/point/{id}` | 특정 사용자의 포인트를 조회합니다. |
| `PATCH` | `/point/{id}/charge` | 사용자의 포인트를 충전합니다. |
| `PATCH` | `/point/{id}/use` | 사용자의 포인트를 사용합니다. |
| `GET` | `/point/{id}/histories` | 사용자의 포인트 충전/사용 내역을 조회합니다. |
