# 물품 도메인 Entity 정책

- 상태: 확정 (2026-09-21)
- 범위: `items`, `item_views`, `item_likes`, `item_stats`, `images`, `group_items`
- 보류: `items_cash`
- 기준 자료:
  - 사용자 제공 ERD SQL 첨부
  - [API 명세 시트](https://docs.google.com/spreadsheets/d/1wpmxCn2-4M9z9CI3jKXCbItgmV6d0PRqrLsxyIEu2kE/edit?gid=0#gid=0)

## 결정

1. Entity의 테이블·컬럼·자료형·PK·FK·널 여부는 ERD SQL을 기준으로 한다.
2. 물리 테이블명은 소문자를 사용한다. 물품 통계·조회 테이블은 `item_stats`, `item_views`다.
3. 일반 FK는 단방향 `LAZY @ManyToOne`으로 매핑한다. 양방향 관계와 cascade는 추가하지 않는다.
4. `item_stats.item_id`는 PK이면서 FK이고 아이템당 통계 한 행이므로 `@OneToOne` 공유 PK로 매핑한다.
5. `Image`는 추후 물품·신고·문의에서 공통으로 사용할 수 있도록 `image` 패키지에서 관리한다. 이미지에는 소유자와 nullable `item_id`를 두고, 하나의 이미지는 하나의 `Item`에만 연결한다. `report_id`, `inquiry_id`는 nullable ID로 보존한다.
6. `items_cash`는 이번 단계에서 구현하지 않는다. 추후 테이블명은 `items_cash`로 통일한다.
7. `item_state`는 `ItemState` enum으로 관리하고 `EnumType.STRING`으로 저장한다. 허용 상태는 `AVAILABLE`(거래가능)과 `COMPLETED`(거래완료)다.
8. `BaseEntity`는 `created_at`만 제공하고, `updated_at`은 필요한 Entity만 `UpdatableEntity`를 통해 선택적으로 제공한다. 물품 범위에서는 `items`, `group_items`가 `SoftDeletableEntity`, `item_views`가 `UpdatableEntity`, `item_stats`, `item_likes`, `images`가 `BaseEntity`를 상속한다.

현재 `SoftDeletableEntity`는 `UpdatableEntity`를 함께 상속하므로 `items`, `group_items`에는 `updated_at`이 매핑된다. 첨부 DDL에서 일부 테이블에 `updated_at`이 빠져 있으므로, 실제 schema/migration 작성 시 Entity 매핑과 일치하는지 확인한다.

## 자료 간 역할

- ERD SQL: 저장 구조와 관계의 기준
- API 시트: endpoint, request/response, validation 계약의 기준
- 기존 코드: `BaseEntity`, `SoftDeletableEntity`, 응답 형식과 JPA 작성 방식의 기준

API 시트의 `item_101` 같은 문자열 ID와 vision confidence·가격 추정 결과는 API 예시/DTO 값이지, Entity 컬럼으로 자동 복사하지 않는다. 내부 식별자는 DDL과 기존 Java 코드에 맞춰 `Long`을 사용한다.

## 보류 사항

- DDL의 `Items_cash.Field`는 의미가 불명확하므로 매핑하지 않는다.
- DDL에 반복된 `users` PK 선언은 물품 도메인 범위가 아니므로 수정하지 않는다.
- `images`의 report/inquiry 관계와 다형성 FK 무결성 보장 방식은 해당 도메인 구현 시 결정한다.
- 가격 변경, 상태 전이, 조회수·좋아요 증감 규칙은 service/use case 구현 전에 별도로 결정한다.

## 완료 기준

- 6개 Entity가 소문자 테이블명과 DDL 컬럼/널/기본값에 맞게 매핑된다.
- 일반 FK는 LAZY 단방향 관계이고 `item_stats`는 공유 PK 관계다.
- `items_cash` 보류 이유와 미결 정책이 이 문서에 남아 있다.
- 프로젝트 build와 기존 test가 통과한다.
