# 물품 상세 조회 Redis 캐시 설계

> 상태: 1차 구현과 로컬 서비스 비교 측정 완료. 실제 API·운영 환경의 p95 개선 효과와 데이터 신선도는 아직 검증하지 않았다. 합의한 정책은 `확정`, 초기 시험에만 쓸 값은 `임시 제안`으로 표시한다.

## 목표와 현재 상태

- **목표 — 확정:** 캐싱을 학습 과제로 진행하면서 물품 상세 조회의 사용자 경험을 개선한다.
- **제약 — 확정:** 현재 사용자는 적고 병목은 측정되지 않았다. 캐시 적용 전후를 같은 조건에서 측정한다.
- **현재 코드:** Redis에는 가격 산정 결과와 인증·상태·순위 등 다른 데이터가 있다. 물품 상세는 공유 가능한 기본 필드만 `item:detail:core:{itemId}` 키에 JSON String으로 캐시한다.
- **응답 구성:** `GET /items/{itemId}`는 물품 정보 외에 그룹 상태, 소유자 정보, 이미지, 좋아요·조회·교환 요청 수, 사용자별 좋아요 여부와 개인화 추천을 함께 조립한다.

## 질문과 답변

### 왜 Redis에 캐시하나요?

**답변 — 확정:** 상세 조회의 반복 DB 작업을 줄일 수 있는지 학습하고 사용자 경험 개선 가능성을 확인하기 위해서다. 아직 병목이나 개선 효과가 측정되지 않았으므로 성능 향상을 전제로 하지 않는다.

### 왜 일반 목록이나 인기순 결과 대신 상세 조회부터 보나요?

**답변 — 설계 범위:** 이 문서는 물품별 상세 조회를 첫 캐시 후보로 다룬다. 상세 조회는 `itemId` 단위로 키를 정할 수 있다. 목록은 검색어·필터·정렬·페이지 조합마다 키가 늘고, 인기순 결과는 조회 이벤트 반영에 맞춘 갱신이 필요하다. 목록과 인기순 캐시는 이번 범위에 포함하지 않는다.

### 무엇을 측정했고, 결과는 어떤가요?

**측정 범위:** 로컬 임베디드 H2와 Docker Redis에서 `ItemQueryService.findDetail`을 호출했다. HTTP 서버·인증·응답 직렬화는 거치지 않았고, 캐시 없는 경로는 mock 캐시가 항상 miss를 반환하도록 구성했다. 따라서 이 결과는 실제 API의 적용 전 기준선이나 운영 성능을 대신하지 않는다.

각 경로를 순차 요청 1,000회 측정했고, 별도로 동시성 8에서 경로별 800회를 측정했다. 모든 수치는 단일 로컬 실행 결과다.

| 경로 | 순차 p50/p95/p99 | DB statement/요청 | 동시성 8 p95 | 동시성 8 처리량 |
|---|---:|---:|---:|---:|
| 캐시 미사용 DB 경로 | 0.386 / 0.713 / 1.635 ms | 7 | 1.677 ms | 13,678.87 req/s |
| cold miss + 채우기 | 1.331 / 3.861 / 7.425 ms | 7 | 미측정 | 미측정 |
| warm cache hit | 0.444 / 0.836 / 1.214 ms | 6 | 3.087 ms | 6,087.01 req/s |

캐시 미스 1,000회와 hit 1,000회가 확인됐고, Redis 오류와 DB fallback은 각각 0회였다. warm hit는 요청당 DB statement를 7개에서 6개로 줄였지만, 이 로컬 측정에서는 p95가 캐시 미사용 경로보다 약 17% 느렸다. 동시 요청에서도 캐시 hit 처리량이 낮았다. H2 조회 비용보다 Docker Redis 왕복 비용이 크게 나타난 결과로 볼 수 있으며, 성능 개선을 입증하지는 못한다.

Redis JSON 값은 280 bytes, `MEMORY USAGE`는 키당 368 bytes, 측정 직후 남은 TTL은 299초였다. 전용 테스트 Redis에서 `maxmemory=0`, `noeviction`, `evicted_keys=0`이었다. Micrometer 명령 타이머는 전체 실행 중 GET 3,003회 평균 0.545 ms, SET 1,102회 평균 0.604 ms를 기록했다. 카운트에는 워밍업과 동시 요청이 포함된다.

운영 설정에는 Prometheus 노출과 `http.server.requests` 히스토그램이 있다. 현재 운영 기준선, 실제 MySQL 쿼리 시간·부하, API 오류율 및 운영 메모리 영향은 측정하지 않았다. **성공 기준은 p95 응답시간 우선, DB 부하 보조로 확정**하며, 실제 API 환경에서 같은 요청·데이터·동시성 조건으로 다시 비교한 뒤 효과를 판단한다.

과거 성과 기록에는 Kafka Fan-out 전후 상세 API p95가 9.48ms에서 5.48ms로 바뀐 결과가 있다. 이는 Kafka 비동기 처리 변경 결과이므로 이번 캐시 실험의 기준선이나 캐시 효과로 사용하지 않는다. ([2026-10-16 v1 기록](https://docs.google.com/document/d/14iT2sxfOdkh9VUHAghof-HsXrwUJwMGhNQyDtiwvQQg/edit))

**후속 측정 절차 — 확정:** 로컬 서비스 비교는 완료했지만 실제 적용 전 API 기준선은 확보하지 못했다. 운영 반영 여부를 판단할 때 실제 API와 DB에서 같은 데이터 세트·요청 구성·동시성·측정 시간을 맞춰 비교한다. cold miss와 warmed hit는 따로 기록한다. p50/p95/p99, 요청량, 오류율, DB 쿼리 수·시간을 기록하고, 캐시 적용 후에는 Redis hit/miss·오류·fallback·명령 지연·메모리·eviction도 함께 비교한다.

### 상세 응답 전체를 캐시하나요?

**답변 — 확정:** 아니다. 공유 가능한 물품 기본 필드만 캐시한다. 사용자별 값과 변동 값은 요청마다 조회해 응답 DTO를 조립한다.

**첫 구현의 필드 경계:**

- Redis String에 JSON으로 저장: 소유자 ID, 제목, 내용, 수량, 상태, 썸네일 이미지 ID, 작성·수정 시각, 교환 점수
- 요청마다 조회·생성: 소유자 프로필, 그룹 접근 상태, 이미지 정보와 presigned URL, 좋아요 여부, 통계·교환 요청 수, 사용자별 추천
- 이미지 presigned URL은 만료되므로 캐시하지 않고 요청 시 생성한다. 이미지 정보도 첫 구현에서는 매 요청 DB에서 조회한다.
- 현재 `ItemDetailResponse`에는 가격 산정 결과 필드가 없다. 향후 가격 필드를 응답에 추가하면 캐시 범위와 무효화 경로를 함께 갱신한다.

여기서 “안정”은 값이 절대 바뀌지 않는다는 뜻이 아니라, 여러 사용자가 공유할 수 있는 물품 기본 정보라는 뜻이다. 값이 바뀌면 캐시를 무효화한다.

### 읽기 전략은 무엇인가요?

**답변 — 확정:** Cache-Aside를 사용한다.

1. `item:detail:core:{itemId}` 키를 Redis에서 조회한다.
2. 적중하면 캐시된 물품 기본 필드를 사용한다.
3. 미스면 DB에서 기본 필드를 읽고 Redis에 TTL과 함께 저장한다.
4. 요청별·변동 필드를 별도로 조회하고 기존 상세 응답 DTO로 합친다.

```text
core = redis.get(key)
if core is missing:
    item = database.findItem(itemId)
    core = itemDetailCacheValue(item)
    redis.set(key, json(core), ttl)
    owner = item.owner
else:
    owner = database.findUser(core.ownerUserId)

dynamic = queryGroupsStatsLikeAndRecommendations(userId, itemId)
return assembleDetailResponse(core, owner, dynamic)
```

**답변 — 확정:** Redis 읽기 실패 시 캐시를 우회해 DB에서 읽고, Redis 저장 실패는 응답 실패로 처리하지 않는다. DB 조회도 실패하면 기존 오류 처리를 따른다. 캐시는 원본이 아니며, Redis 장애가 상세 조회 자체의 실패로 바로 이어지지 않도록 한다. Redis 장애 중 fallback 요청이 늘면 DB 부하와 오류율을 관찰한다.

### 쓰기 전략은 무엇인가요? Write-Through인가요?

**답변 — 확정:** DB를 먼저 갱신하고, 트랜잭션 커밋 후 해당 상세 캐시 키를 삭제한다. 다음 읽기가 DB에서 최신 데이터를 다시 채운다. 이는 **Cache-Aside 읽기 + 커밋 후 캐시 무효화**이며 Write-Through가 아니다. DB 변경과 Redis 쓰기를 하나의 원자적 쓰기로 동기화하지 않기 때문이다.

```text
DB transaction: item update or soft delete
after successful commit: DEL item:detail:core:{itemId}
```

제목·내용·수량·상태·썸네일 이미지 ID 등 캐시에 담은 값이 바뀌는 모든 쓰기 경로에서 같은 키를 무효화해야 한다. `ItemService.update`와 `delete`뿐 아니라, 교환 완료로 수량·상태를 바꾸는 `ExchangeRequestService.updateStatus`도 요청 물품과 제공 물품 키를 무효화한다. 삭제는 soft delete 후에도 이전 상세 캐시가 남아 조회될 수 있으므로 무효화가 특히 중요하다. 가격 산정 값은 현재 상세 DTO에 없으므로 현 캐시에서는 무효화 대상이 아니며, 상세 응답에 포함하게 되면 가격 결과 커밋 경로도 무효화에 추가한다.

코드에 이미 `ItemPriceResultService`가 DB 커밋 후 `TransactionSynchronization.afterCommit`에서 가격 캐시를 채우는 패턴이 있다. 상세 캐시 삭제도 같은 커밋 경계를 활용할 수 있다. DB 커밋 이후 Redis 삭제가 실패해도 이미 성공한 DB 쓰기를 실패로 되돌리지 않고, 오류를 기록한다. 별도 재시도 보장은 이번 설계에 포함하지 않으며, 기존 키의 남은 TTL(초기 설정 기준 최대 5분)이 오래된 값의 상한이 된다.

### TTL과 메모리 증가는 어떻게 다루나요?

**답변 — 임시 제안 확정:** 요청된 물품만 캐시에 올리고 모든 캐시 키에 유한 TTL을 둔다. 초기 시험 TTL은 5분으로 시작한다. TTL은 무효화 누락이나 Redis 삭제 실패 때 오래된 값이 계속 남는 시간을 제한한다. 이 값은 운영 최종값이 아니며, 기준선·데이터 변경 빈도·신선도 요구를 확인한 뒤 조정한다.

애플리케이션은 `item.detail.cache` hit/miss/error/fallback 카운터와 `item.detail.cache.command` Redis 명령 타이머를 노출한다. Redis 메모리 사용량, 키 수와 eviction은 Redis 운영 지표로 관찰한다. Redis 정책을 바꿔야 할 정도로 메모리 압박이 확인되면 상세 캐시를 별도 Redis 인스턴스로 분리한 뒤 그 인스턴스에 LRU를 적용하는 방안을 검토한다.

**Redis 운영 방식 — 확정:** 초기에는 기존 Redis 인스턴스에서 `item:detail:core:` prefix와 유한 TTL을 사용한다. 현재 적용된 eviction 설정은 바꾸지 않는다. LRU와 `volatile-*` 정책은 전체 설계를 마치고 현재 설정과 메모리 상황을 확인한 뒤 다시 결정한다.

LRU는 `item:detail:*` 접두사에만 따로 설정하는 옵션이 아니다. `maxmemory-policy`는 Redis 인스턴스의 eviction 정책이다. 현재 인스턴스에 인증·세션 등 캐시 외 데이터도 있으므로 `allkeys-lru`를 전역 적용하면 그런 키도 제거될 수 있다. TTL이 붙은 키에만 적용하는 정책 역시 TTL이 있는 다른 상태 키를 제거할 수 있다. Redis 문서도 혼합 용도의 단일 인스턴스보다 별도 인스턴스를 고려하도록 안내한다. ([Redis eviction 정책 문서](https://redis.io/docs/latest/develop/reference/eviction/))

### 구현은 어느 코드에 들어갔나요?

**1차 구현:**

1. `ItemDetailCacheValue`에 첫 구현에서 공유할 물품 필드를 담는다.
2. `RedisItemDetailCache`가 String 키에 JSON을 저장하고 5분 TTL, hit/miss/error/fallback 카운터, 명령 타이머, 커밋 후 삭제를 처리한다.
3. `ItemQueryService.findDetail`은 hit 시 캐시 기본 필드와 DB의 최신 소유자 정보를 사용하고, miss 시 물품을 DB에서 읽어 캐시를 채운다. 그룹·이미지·통계·좋아요·추천은 매 요청 조회해 `ItemDetailResponse`를 조립한다.
4. `ItemService.update`·`delete`와 교환 완료 경로에서 DB 커밋 후 `item:detail:core:{itemId}`를 삭제한다.
5. 로컬 서비스 비교를 완료했다. 실제 API 적용 전후 p95와 DB 쿼리 수·시간 비교, 오류율·fallback·데이터 신선도·Redis 메모리 확인은 운영 측정 과제로 남아 있다.

## 남은 결정

- 캐시 전용 값에 포함할 필드의 최종 목록
- 상세 캐시 TTL과 Redis 메모리 경보 기준
- 기준선과 데이터 변경 빈도 확인 후 운영 TTL 확정
- LRU와 eviction 설정을 다시 검토할 시점과 분리 기준
- 캐시 미스 동시 조회나 무효화 타이밍 경쟁으로 오래된 값을 다시 채우는 상황을 어느 수준까지 방어할지
