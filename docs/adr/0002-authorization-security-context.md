# ADR-0002: 권한 처리와 SecurityContext

- 상태: Accepted
- 결정일: 2026-09-16
- 구현 상태: Access Token 발급 및 요청 인증 필터 구현 완료

## 결정

JWT의 `role`은 권한 정보를 전달하는 claim으로 사용하고, 요청마다 JWT 서명과 만료 시간을 검증한 뒤 `SecurityContext`에 인증 정보를 저장한다.

`role`을 단순히 요청 파라미터나 쿠키에서 읽어 권한을 결정하지 않는다. 클라이언트가 JWT payload를 읽을 수는 있지만, 서명 검증 없이 수정된 payload를 신뢰해서는 안 된다.

요청 처리 순서는 다음과 같다.

1. `Authorization: Bearer <access-token>`에서 토큰을 추출한다.
2. `JwtDecoder`가 JWT 서명과 시간 관련 claim(`exp`, `nbf`, `iat`)을 검증한다.
3. 검증된 `sub`를 사용자 식별자로 사용하고 `role`을 `GrantedAuthority`로 변환한다.
4. 변환한 인증 객체를 현재 요청의 `SecurityContext`에 저장한다.
5. Controller 또는 method security는 `SecurityContext`의 authority만 사용해 접근을 판단한다.

개념적인 변환 결과는 다음과 같다.

```java
String role = jwt.getClaimAsString("role");
Authentication authentication = new UsernamePasswordAuthenticationToken(
	jwt.getSubject(),
	null,
	List.of(new SimpleGrantedAuthority("ROLE_" + role))
);
SecurityContextHolder.getContext().setAuthentication(authentication);
```

현재 구현은 [JwtAuthenticationFilter](../../src/main/java/com/example/KTB_Agile_backend/security/JwtAuthenticationFilter.java)가 직접 처리한다. 필터는 `JwtDecoder`로 토큰을 검증하고, `sub`가 양의 정수인지 확인한 뒤 principal 문자열로 사용한다. `role`은 `UserRole` enum으로 변환해 허용된 역할만 통과시키고 `ROLE_` prefix를 붙인 authority를 생성한다.

`JwtAccessTokenIssuer`와 `SecurityConfig`는 같은 HMAC secret과 HS256 알고리즘을 사용한다. 현재 access token에는 issuer claim을 넣지 않으므로 issuer 검증은 하지 않는다. issuer를 도입할 때 발급과 검증 설정을 함께 변경한다.

`hasRole("ADMIN")`을 사용할 때는 `ROLE_ADMIN` authority가 생성되도록 prefix를 일관되게 유지한다.

별도의 `CurrentUser` 클래스는 현재 두지 않는다. 현재 principal로 `sub` 문자열을 사용하면 `Authentication#getName()`으로 사용자 ID를 얻을 수 있다. 여러 Controller에서 ID와 role을 함께 다루는 요구가 생기면 그때 `CurrentUser` 같은 타입을 추가한다.

## 이 구조를 사용하는 이유

- JWT claim은 서명된 인증 결과지만, 그 자체가 현재 요청의 인증 객체는 아니다.
- `SecurityContext`는 요청 단위로 인증 주체와 authority를 보관하는 Spring Security 표준 진입점이다.
- `SecurityConfig`는 세션을 사용하지 않는 stateless 정책을 적용하고, `/auth/oauth/**`, `/auth/refresh`, `/error`만 공개한다. `/auth/logout`은 인증이 필요하다.
- Controller가 JWT 문자열을 직접 파싱하지 않아도 `@PreAuthorize("hasRole('ADMIN')")` 같은 공통 권한 처리를 사용할 수 있다.

## 역할 변경과 Access Token 만료

JWT에 담긴 role은 발급 시점의 값이다. 관리자가 사용자의 권한을 변경해도 이미 발급된 Access Token은 만료 전까지 남아 있을 수 있다. 현재 Access Token 만료 시간은 15분으로 두고, 즉시 권한 회수가 필요하면 다음 중 하나를 추가한다.

- 권한이 중요한 요청에서 DB의 현재 role/status를 재확인한다.
- 짧은 Access Token 수명과 Refresh Token 폐기를 함께 사용한다.
- 토큰 버전 또는 폐기 목록을 도입한다.

이번 범위에서는 짧은 Access Token과 DB에 해시로 저장하는 Refresh Token까지만 구현한다. 토큰 폐기 목록은 추가하지 않는다.
