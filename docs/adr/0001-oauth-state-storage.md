# ADR-0001: OAuth state 저장 방식

- 상태: Accepted
- 결정일: 2026-09-16
- 구현 상태: 별도 PR 예정

## 배경

OAuth의 `state`는 인증 요청과 callback을 연결하고 CSRF 공격을 방지하는 일회성 값이다. 현재 1차 구현에서는 Redis를 사용하지 않기로 했지만, state의 만료와 재사용 방지는 서버 측에서 관리해야 한다.

## 결정

1. state 저장소의 추상화는 `auth/state/OAuthStateStore` 인터페이스로 둔다.
2. 1차 구현은 `RdbOAuthStateStore`를 사용하고, 이후 `RedisOAuthStateStore`를 추가한다.
3. 관련 코드는 사용자 도메인이 아닌 `auth` 아래에 둔다.

```text
auth/
├── entity/OAuthState.java
├── repository/OAuthStateRepository.java
└── state/
    ├── OAuthStateStore.java
    ├── RdbOAuthStateStore.java
    └── RedisOAuthStateStore.java  # 추후 추가
```

RDB 테이블은 다음 구조를 사용한다.

```text
oauth_states
- oauth_state_id BIGINT PK
- state_hash CHAR(64) UNIQUE NOT NULL
- provider VARCHAR(20) NOT NULL
- expires_at TIMESTAMPTZ NOT NULL
- created_at TIMESTAMPTZ NOT NULL
- consumed_at TIMESTAMPTZ NULL
```

- 원본 state는 응답과 HttpOnly 쿠키에 전달한다.
- RDB에는 원본이 아닌 SHA-256 해시만 저장한다.
- 로그인 시 쿠키의 state와 요청 state를 비교한 뒤, 만료되지 않고 아직 소비되지 않은 값을 원자적으로 1회 소비한다.
- state는 인증 전에 발급되므로 `user_id` 외래키를 두지 않는다.

## 결과

### 장점

- 인증 서비스는 `OAuthStateStore`만 의존하므로 RDB에서 Redis로 교체할 때 영향 범위를 줄일 수 있다.
- DB 유출 시에도 저장된 값만으로 원본 state를 복원할 수 없다.
- `consumed_at` 조건을 포함한 원자적 소비로 state 재사용을 막을 수 있다.

### 부담

- RDB 구현에서는 만료되거나 소비된 row를 정리해야 한다.
- Redis 전환 시 TTL과 원자적 삭제(`GETDEL` 또는 동등한 방식)를 동일한 계약으로 맞춰야 한다.

## 고려한 대안

- **쿠키만 사용**: 별도 저장소가 없어 단순하지만 서버 측 1회 소비와 강제 폐기가 어렵기 때문에 선택하지 않았다.
- **처음부터 Redis 사용**: 최종 방향에는 맞지만 1차 인프라 범위에서 제외했다.
- **원본 state 저장**: 조회는 쉽지만 DB 유출 시 노출 위험이 있어 선택하지 않았다.
