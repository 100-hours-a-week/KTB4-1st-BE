# 그룹명 중복과 soft delete 처리

## 확정된 결정

- 운영 데이터베이스는 MySQL을 사용한다.
- ORM은 JPA를 사용한다.
- 활성 그룹(`deleted_at IS NULL`)끼리는 그룹명이 완전히 같을 수 없다.
- soft delete된 그룹의 이름은 새 그룹에서 다시 사용할 수 있다.
- 삭제된 그룹의 `group_name` 원본은 이력과 상품 작성자 표시를 위해 변경하지 않는다.
- `UNIQUE(group_name, deleted_at)`은 사용하지 않는다.
- `active_group_name`을 JPA 엔티티에 매핑하고 unique index를 적용한다.
- 그룹의 soft delete 트리거는 마지막 `ACTIVE` 멤버 탈퇴이며, 소유자·관리자에 의한 수동 삭제는 없다.
- soft-deleted 그룹은 복구하지 않으며, 필요하면 같은 이름으로 새 그룹을 생성한다.

## `UNIQUE(group_name, deleted_at)`을 피하는 이유

이 제약은 “`deleted_at`이 null인 행 중에서 `group_name`을 unique하게 만든다”는 조건부 unique가 아니다. 두 컬럼의 조합 전체를 unique하게 만들 뿐이다.

예를 들어 활성 그룹 두 개가 같은 이름으로 생성되면 데이터는 다음처럼 저장된다.

| 행 | `group_name` | `deleted_at` |
| --- | --- | --- |
| A | 우리동네 | `NULL` |
| B | 우리동네 | `NULL` |

MySQL의 unique index에서는 `NULL`을 서로 같은 값으로 비교하지 않으므로 두 행이 모두 허용될 수 있다. 따라서 활성 그룹의 이름 중복을 막지 못한다.

또한 이 제약이 실제로 보장하는 것은 다음에 가깝다.

```text
(group_name, deleted_at) 조합이 같은 행은 허용하지 않음
```

우리가 원하는 조건은 다음이다.

```text
deleted_at IS NULL인 행만 group_name 기준으로 unique
```

두 의미가 다르므로 JPA의 `@UniqueConstraint`로 `(group_name, deleted_at)`을 선언하는 것은 해결책이 아니다. 데이터베이스마다 `NULL`의 unique 처리와 조건부 index 문법도 다르다.

## 실무 해결 방법

### 1. Partial 또는 filtered unique index

PostgreSQL과 SQL Server처럼 조건부 unique index를 지원하는 데이터베이스라면 가장 의미가 정확하다.

PostgreSQL 예시:

```sql
CREATE UNIQUE INDEX ux_groups_active_name
    ON groups (group_name)
    WHERE deleted_at IS NULL;
```

장점은 보조 컬럼 없이 요구사항을 그대로 DB에 표현한다는 점이다. 단점은 데이터베이스별 문법이므로 JPA annotation만으로 표현할 수 없다는 점이다. MySQL에서는 이 문법을 그대로 사용할 수 없다.

### 2. Generated column에 unique index 적용

MySQL에서는 삭제되지 않은 행의 이름만 generated column에 노출하고, 그 컬럼에 unique index를 둘 수 있다.

```sql
ALTER TABLE groups
    ADD COLUMN active_group_name VARCHAR(30)
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted_at IS NULL THEN group_name
                ELSE NULL
            END
        ) STORED;

CREATE UNIQUE INDEX ux_groups_active_name
    ON groups (active_group_name);
```

이 방식은 MySQL 전용 migration이 필요하다. JPA annotation만으로 generated column과 expression index를 선언하기 어렵다는 점이 단점이다.

### 3. JPA 엔티티에 보조 unique key 컬럼을 매핑하는 방법 — 확정안

MySQL과 JPA를 운영 기준으로 삼으므로 `active_group_name`을 일반 nullable 컬럼으로 두고 JPA 엔티티에 매핑한다. H2 호환성을 위해 별도의 조건부 unique 구현을 추가하지 않는다.

```text
활성 그룹:       active_group_name = 정규화된 group_name
soft delete 시:  active_group_name = NULL
제약:            UNIQUE(active_group_name)
```

엔티티 매핑은 다음과 같이 둘 수 있다.

```java
@Column(name = "active_group_name", length = 30, unique = true)
private String activeGroupName;
```

`unique = true`는 개발 환경의 스키마 생성에 활용하고, 운영 DB에서는 같은 unique index를 migration으로 명시한다. JPA는 컬럼을 관리하고, MySQL은 최종 중복 보장을 담당한다.

`group_name`은 원본을 그대로 보존하고, 중복 판단에만 `active_group_name`을 사용한다. 활성 그룹은 같은 보조 키를 가질 수 없고, 삭제된 그룹은 보조 키가 null이므로 같은 이름을 다시 사용할 수 있다.

구현 규칙은 다음과 같다.

1. `Group.create()`에서 `groupName`과 동일한 비교 기준으로 `activeGroupName`을 채운다.
2. `Group.delete()`에서 `deletedAt`을 설정하고 `activeGroupName`을 null로 만든다.
3. 삭제와 보조 키 변경은 같은 트랜잭션에서 처리한다.
4. 애플리케이션의 사전 중복 조회는 사용자에게 409를 빠르게 반환하기 위한 보조 검사로만 사용한다.
5. 최종 동시성 보장은 DB unique index에 맡기고, unique 위반 예외는 409 Conflict로 변환한다.

현재 생성 요청 DTO는 그룹명의 앞뒤 공백만 제거한다. 대소문자와 내부 공백을 어떻게 비교할지는 별도 정책으로 확정한 뒤, `activeGroupName`, 검색 조건, 테스트에 같은 규칙을 적용해야 한다.

## 권장 테스트

- 활성 그룹 A가 `우리동네`일 때 활성 그룹 B의 같은 이름 생성은 실패한다.
- 그룹 A를 soft delete한 뒤 새 그룹 B를 `우리동네`로 생성할 수 있다.
- 삭제된 그룹 여러 개가 같은 `group_name`을 가져도 저장할 수 있다.
- 동시에 같은 이름을 생성하면 하나만 성공하고 나머지는 unique 위반으로 실패한다.
- soft delete 후에도 그룹 A의 `group_name` 원본은 변하지 않는다.

## 결론

조건부 unique라는 정책 자체는 맞다. 최종 구현은 JPA 엔티티에 `active_group_name` 보조 컬럼을 매핑하고, MySQL unique index로 보장한다. `UNIQUE(group_name, deleted_at)`으로 조건부 unique를 흉내 내지는 않는다.

운영 반영 시에는 Hibernate 자동 스키마 생성에 의존하지 않고 migration으로 컬럼과 index를 추가한다. MySQL의 `NULL`을 포함한 unique index 동작은 [MySQL CREATE INDEX 문서](https://dev.mysql.com/doc/refman/8.0/en/create-index.html), generated column/index 관련 제약은 [MySQL generated-column index 문서](https://dev.mysql.com/doc/refman/8.0/en/generated-column-index-optimizations.html)와 [MySQL secondary index 문서](https://dev.mysql.com/doc/refman/26.7/en/create-table-secondary-indexes.html), PostgreSQL partial unique index는 [PostgreSQL CREATE INDEX 문서](https://www.postgresql.org/docs/current/sql-createindex.html)를 참고한다.
