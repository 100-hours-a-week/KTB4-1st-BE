# 그룹 도메인 아키텍처 (v1)

- 상태: Proposed
- 범위: 그룹 관리의 서버 구조와 첫 구현 순서
- 기준 자료:
  - [API 명세 시트](https://docs.google.com/spreadsheets/d/1wpmxCn2-4M9z9CI3jKXCbItgmV6d0PRqrLsxyIEu2kE/edit?gid=0#gid=0)
  - [ERDCloud](https://www.erdcloud.com/d/miNaQP4YaxBpfsTkg)
  - [Figma v1 그룹 화면](https://www.figma.com/design/Ag5SlDfPN8eQUp2SiBn1Dk/KTB4_1%EC%A1%B0_Figma?node-id=12178-4789&p=f)

## 1. 결론

`group`을 기능 단위 패키지로 추가한다. 현재 코드베이스의 `Controller → Service → Repository → Entity` 흐름을 유지하고, 그룹 엔티티에 JPA 연관관계를 모두 펼치지 않는다.

핵심 결정은 다음과 같다.

1. `Group`은 그룹 정보와 삭제 상태를 소유하는 aggregate root다.
2. `GroupMember`는 가입·탈퇴·위치 인증이라는 별도 생명주기가 있으므로 별도 엔티티로 관리한다.
3. `GroupItem`은 그룹과 물품의 다대다 연결을 나타내는 association entity로 둔다. `@ManyToMany`는 사용하지 않는다.
4. 그룹 생성 시 생성자를 첫 번째 `ACTIVE` 멤버로 함께 저장한다.
5. 도로명 주소 조회는 프론트 책임이다. 백엔드는 전달받은 주소와 좌표의 형식·범위만 검증하고 주소 API를 호출하지 않는다.
6. 화면의 “가입한 그룹 없음”은 서버가 임의로 그룹을 만들거나 가입시키는 상태가 아니다. `GET /users/me/groups`가 빈 배열을 반환하고 프론트가 검색·생성 진입점을 표시한다.

## 2. 현재 자료에서 확인된 범위

Figma v1에는 다음 상태가 있다.

| 화면 | 역할 |
| --- | --- |
| `MEM-GRP-001` | 가입 그룹 목록, 그룹 선택 |
| `MEM-GRP-002` | 가입 그룹이 없을 때 검색·생성 진입 |
| `MEM-GRP-003` | 그룹명 검색 결과 |
| `MEM-GRP-004` | 검색 결과 없음, 그룹 생성 이동 |
| `MEM-GRP-005` | 선택 그룹 참가 확인 |

그룹 생성 화면의 요구는 그룹명, 주소, 위도·경도, 선택 설명이다. 주소 검색과 도로명 API는 프론트에서 처리하고, 선택한 결과를 생성 요청에 넣는다.

시트의 그룹 API 범위는 다음과 같다.

```text
GET    /groups                       그룹 검색
GET    /groups/recommendations       위치 기반 추천
POST   /groups/{id}/location-verifications
POST   /groups/{id}/members          그룹 가입
DELETE /groups/{id}/members/me       그룹 탈퇴
POST   /groups                       그룹 생성
DELETE /groups/{id}                  그룹 삭제
GET    /users/me/groups              내 그룹 목록
```

그룹별 물품 목록·검색은 `item` 도메인이 소유하고, 그룹 도메인은 접근 권한과 `GroupItem` 연결만 제공한다.

## 3. 패키지 구조

처음부터 모든 예정 파일을 만들지 않는다. 아래 구조 중 실제 유스케이스가 들어오는 파일만 추가한다.

```text
group/
├── controller/
│   └── GroupController.java
├── dto/
│   ├── request/
│   │   ├── CreateGroupRequest.java
│   │   ├── JoinGroupRequest.java
│   │   └── LocationVerificationRequest.java
│   └── response/
│       ├── GroupCreatedResponse.java
│       ├── GroupMemberResponse.java
│       ├── GroupPageResponse.java
│       ├── GroupSummary.java
│       └── LocationVerificationResponse.java
├── entity/
│   ├── Group.java
│   ├── GroupMember.java
│   ├── GroupItem.java
│   └── GroupMemberStatus.java
├── repository/
│   ├── GroupRepository.java
│   ├── GroupMemberRepository.java
│   └── GroupItemRepository.java
└── service/
    ├── GroupService.java
    └── GroupQueryService.java
```

`GroupService`는 생성·삭제·가입·탈퇴·위치 인증 같은 상태 변경을 담당하고, `GroupQueryService`는 목록·검색·추천 조회를 담당한다. 서비스가 커지기 전까지 유스케이스마다 클래스를 쪼개지 않는다.

## 4. 도메인 모델

```text
User 1 ───── N GroupMember N ───── 1 Group
                                      │
                                      │ 1
                                      N
                                  GroupItem N ───── 1 Item
```

### `Group`

책임:

- 그룹명·도로명 주소·좌표·설명·생성 시각 관리
- 생성 시 불변식 검증
- `delete()`로 soft delete 상태 전환
- 그룹 위치 기준점 제공

권장 필드:

```text
id              Long
groupName       String       최대 30자
roadAddress     String       최대 100자
longitude       BigDecimal   NUMERIC(9, 6)
latitude        BigDecimal   NUMERIC(9, 6)
groupContent    String       최대 300자, 없으면 빈 문자열
createdAt       LocalDateTime
deletedAt       LocalDateTime nullable
```

엔티티는 공개 setter를 두지 않고 `Group.create(...)`, `delete()`처럼 의도가 드러나는 메서드로 상태를 바꾼다. Controller가 전달한 문자열을 그대로 저장하지 않고 생성 메서드에서 trim·범위 검증을 한 번 더 수행한다.

### `GroupMember`

가입 정보는 그룹 자체와 수명이 다르므로 `Group` 내부의 `List<GroupMember>`로 관리하지 않는다.

```text
id                    Long
group                 Group       LAZY
user                  User        LAZY
locationVerifiedAt    LocalDateTime
status                GroupMemberStatus
createdAt             LocalDateTime
leftAt                LocalDateTime nullable
```

행동:

- `join(verifiedAt)`: `ACTIVE`, `leftAt = null`
- `leave(leftAt)`: `LEFT`, 탈퇴 시각 기록
- `isActive()`

같은 사용자와 그룹의 관계는 한 row로 유지한다. 재가입 시 삭제 후 새 row를 만들지 않고 기존 row를 재활성화한다. 따라서 `UNIQUE(group_id, user_id)`를 유지하면서 `leftAt` 이력은 최신 상태만 보관한다. 과거 가입 이력이 필요해지는 시점에 별도 이력 테이블을 추가한다.

### `GroupItem`

`group_items`는 `group_id`, `item_id`, `created_at`, `deleted_at`을 보유하는 연결 엔티티다. 그룹 엔티티가 물품 엔티티를 직접 로딩하지 않도록 하고, 물품 생성·수정 유스케이스가 그룹 ID 목록을 `GroupService` 또는 `GroupItemRepository`로 검증한다.

## 5. 생성 유스케이스

```text
POST /groups
  → GroupController
  → GroupService.create(userId, request)
      1. 요청값 검증 및 정규화
      2. 활성 그룹 중복 확인
      3. Group 생성·저장
      4. 생성자를 GroupMember(ACTIVE)로 저장
      5. groupId·createdAt 반환
  → 201 + ApiResponse + Location
```

트랜잭션 경계는 `GroupService.create()` 하나다. 그룹 저장은 성공했지만 첫 멤버 저장이 실패하는 반쪽 상태를 허용하지 않는다.

생성자 멤버의 `locationVerifiedAt`은 현재 API 계약으로 결정할 수 없다. 시트의 생성 요청에는 그룹 위치 좌표만 있고, 생성자의 현재 위치 또는 사전 인증 토큰이 없다. 1차 기본안은 생성 요청의 위치를 그룹 기준점으로 사용하고 생성자를 자동 가입시키는 것이다. 물리적 현재 위치 인증이 필수라면 `currentLatitude/currentLongitude` 또는 `locationVerificationToken`을 생성 요청에 추가해야 한다.

## 6. 위치 인증과 가입

### 위치 인증

`POST /groups/{groupId}/location-verifications`는 요청 좌표와 그룹 기준 좌표의 거리를 계산한다.

- 위도·경도 형식과 범위 검증
- Haversine 거리 계산
- 설정된 허용 반경 이내면 `verified = true`
- 성공 시 사용자·그룹·용도·만료 시각이 포함된 짧은 수명의 서명 토큰 발급
- 반경 밖이면 200과 `verified = false` 반환

토큰은 평문 UUID가 아니라 서명된 토큰이어야 한다. 1차에는 별도 인증 테이블을 만들지 않고 짧은 수명의 서명 토큰으로 처리한다. 토큰을 반드시 1회만 사용해야 하는 요구가 생기면 `group_location_verifications` 테이블을 추가해 사용 여부를 저장한다.

허용 반경과 토큰 TTL은 설정값으로 둔다. 실제 값은 제품 정책 결정 후 넣는다.

### 가입

`POST /groups/{groupId}/members`는 다음을 한 트랜잭션에서 검사한다.

1. 그룹이 존재하고 `deletedAt`이 null인지 확인
2. 토큰의 서명·용도·사용자 ID·그룹 ID·만료 시각 확인
3. 사용자의 활성 그룹 수가 5개 미만인지 확인
4. 이미 `ACTIVE` 멤버면 409
5. `LEFT` 멤버면 기존 row 재활성화

동시 가입으로 5개 제한을 우회하지 않도록 활성 멤버 수 확인과 멤버 저장을 같은 트랜잭션에 둔다. 트래픽이 커져 경쟁이 실제 문제가 되면 사용자별 잠금 또는 별도 quota 모델을 추가한다.

## 7. 조회 유스케이스

`GroupQueryService`는 Entity 목록을 Controller에 반환하지 않고 projection/DTO로 바로 변환한다.

### 내 그룹 목록

`GET /users/me/groups`는 `GroupMember(status = ACTIVE)`와 활성 `Group`을 조인해 반환한다. 결과가 없으면 오류가 아니라 `groups: []`, `hasNext: false`다.

### 그룹 검색

`GET /groups?keyword=&size=&cursor=`는 그룹명 기준으로 검색한다.

- keyword 앞뒤 공백 제거
- size는 1~50
- cursor는 opaque 값으로 외부에 내부 ID를 노출하지 않음
- 삭제된 그룹은 제외
- `isJoined`는 현재 사용자 멤버십으로 계산

Figma의 `isCurrentGroup`은 현재 시트 API 요청에 현재 그룹 식별자가 없고 ERD에도 사용자별 현재 그룹 저장 컬럼이 없다. 따라서 1차에는 프론트가 선택 그룹과 `groupId`를 비교한다. 서버 필드가 반드시 필요하면 `currentGroupId`를 요청에 추가하거나 사용자 설정 모델을 별도로 정의한다.

### 위치 기반 추천

`GET /groups/recommendations`는 요청 좌표와 그룹 좌표의 거리를 기준으로 조회한다. 검색·내 그룹 목록의 cursor가 ID 하나로 충분하더라도 추천 정렬은 거리와 ID를 함께 커서에 담아야 한다. API의 cursor 문자열은 이 내부 구조를 숨긴다.

그룹의 `memberCount`, `itemCount`, `lastItemCreatedAt`은 조회 projection에서 집계한다. 그룹 Entity에 컬렉션을 eager loading하지 않는다.

## 8. ERD와 보강 사항

현재 ERD의 핵심 컬럼은 다음과 같다.

```text
groups
- group_id, group_name, road_address
- group_longitude, group_latitude
- group_content, created_at, deleted_at

group_members
- group_members_id, group_id, user_id
- location_verified_at, user_status
- created_at, left_at

group_items
- group_item_id, group_id, item_id
- created_at, deleted_at
```

구현 전에 다음 제약을 migration에 명시한다.

```text
group_members: UNIQUE(group_id, user_id)
group_items:   UNIQUE(group_id, item_id)
group_members: INDEX(user_id, user_status, group_members_id)
group_items:   INDEX(group_id, deleted_at, item_id)
```

추가로 그룹 삭제 권한을 판별할 소유자 정보가 ERD에 없다. 생성자를 “가장 먼저 가입한 멤버”로 추정하면 순서·재가입·동시성에 취약하다. 권장안은 `groups.owner_user_id NOT NULL`을 추가하는 것이다. 컬럼을 추가하지 못하면 그룹 삭제 API는 구현하지 않고, `GroupMember`에 명시적인 `member_role`을 추가하는 대안을 먼저 결정한다.

`group_content`는 ERD에서 NOT NULL이지만 API·Figma에서는 선택사항이다. 요청의 null/blank를 빈 문자열로 정규화해 DB 제약을 그대로 유지한다.

## 9. API 응답·예외

현재 공통 응답인 `ApiResponse<T>`와 `ApiException`을 재사용한다. 그룹 전용 예외 타입을 만들지 않는다.

공통 `ErrorCode`에는 다음 값이 필요하다.

```text
NOT_FOUND           404
CONFLICT            409
UNPROCESSABLE_ENTITY 422
```

| 상황 | 코드 |
| --- | --- |
| ID·문자열·좌표·size·cursor 형식 오류 | `BAD_REQUEST` |
| 그룹·멤버를 찾지 못함 | `NOT_FOUND` |
| 같은 그룹 중복, 이미 가입, 최대 5개 초과 | `CONFLICT` |
| 생성 시 물리적 위치 인증을 별도로 요구할 때 미검증 | `UNPROCESSABLE_ENTITY` |
| 위치 인증 반경 밖 | 200 + `verified: false` |

`GlobalExceptionHandler`의 URI별 500 메시지에는 최종 경로(`/groups`, `/users/me/groups`)를 결정한 후 그룹 메시지를 추가한다.

## 10. 테스트 순서

1. `Group` 엔티티: 생성 불변식, trim, 좌표 범위, 설명 기본값, soft delete
2. `GroupMember` 엔티티: 가입·탈퇴·재가입 상태 전환
3. `GroupService` 단위 테스트: 생성자 멤버 동시 저장, 중복 그룹, 최대 그룹 수, 권한
4. `GroupQueryService`/Repository 테스트: 빈 목록, 삭제 그룹 제외, 검색 cursor, 가입 여부
5. `GroupController` 테스트: `ApiResponse`, 201 Location, 400/404/409/422
6. H2 통합 테스트: unique 제약과 트랜잭션 rollback

첫 구현에서 가장 작은 의미 있는 검증은 `GroupService.create()`가 그룹과 생성자 멤버를 함께 저장하고, 중복 요청에는 409를 반환하는 테스트다.

## 11. 구현 순서

### 1단계 — 생성과 빈 상태

- `Group`, `GroupMember`, 상태 enum
- Repository
- `POST /groups`
- `GET /users/me/groups`
- 생성자 자동 가입

### 2단계 — 검색과 참가

- `GET /groups`
- 위치 인증 토큰
- `POST /groups/{id}/location-verifications`
- `POST /groups/{id}/members`
- `DELETE /groups/{id}/members/me`

### 3단계 — 추천·삭제·물품 연계

- 위치 기반 추천
- 소유자 권한이 확정된 뒤 그룹 삭제
- `GroupItem` 및 item 도메인 연계

## 12. 구현 전 결정 필요

아래 항목은 코드 위치가 아니라 API·DB 계약을 바꾸므로 먼저 합의한다.

1. **경로 prefix**: 시트의 `/api/groups`를 전역 context path로 적용할지, 현재 코드처럼 `/groups`로 구현할지
2. **그룹 중복 기준**: Figma의 그룹명 단독인지, 시트의 그룹명+도로명 주소 조합인지
3. **생성 위치 인증**: 생성 요청 좌표를 그룹 기준점으로만 볼지, 생성자의 현재 위치 인증까지 요구할지
4. **그룹 삭제 권한**: `owner_user_id`를 ERD에 추가할지, `GroupMember`에 역할을 추가할지
5. **위치 인증 반경·토큰 TTL**: 제품 정책 값
6. **생성 응답의 Location**: `/groups/{id}` 상세 조회 API를 함께 만들지, 존재하는 리소스 경로로 바꿀지
