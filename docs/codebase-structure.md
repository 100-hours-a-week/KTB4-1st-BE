# 코드베이스 구조와 클래스 역할

이 문서는 현재 `src/main/java`의 구현을 기준으로 KTB Agile Backend의 파일, 클래스, 인터페이스, 주요 메소드의 역할을 정리한다.

> `record`의 컴포넌트 접근자, Lombok이 생성하는 getter/생성자, Spring Data JPA가 상속으로 제공하는 기본 CRUD 메소드는 직접 선언된 메소드와 구분해 설명한다.

## 1. 전체 구조

### 패키지별 책임

```text
HTTP 요청
  -> Controller
  -> Service
  -> Provider Client / State Store / Token Issuer / Repository
  -> Entity 및 DB
```

| 패키지 | 책임 |
| --- | --- |
| `auth/controller` | OAuth 상태 발급, Kakao callback·로그인, 토큰 재발급, 로그아웃 HTTP API |
| `auth/service` | 인증 유스케이스 조합, OAuth state와 Refresh Token 처리, 해시 유틸리티 |
| `auth/client` | OAuth provider와 통신하고 provider 응답을 공통 사용자 정보로 변환 |
| `auth/dto` | 요청·응답 DTO와 Kakao 외부 API 매핑 DTO |
| `auth/entity`, `auth/repository` | OAuth state의 DB 모델과 조회·일회성 소비 |
| `auth/state` | OAuth state 저장소 추상화와 RDB 구현 |
| `auth/token` | Access Token 발급 추상화와 JWT 구현 |
| `user/controller` | 인증된 사용자의 회원 탈퇴 API |
| `user/service` | 소셜 계정 기반 사용자 생성·조회 및 탈퇴 |
| `user/entity`, `user/repository` | 사용자·소셜 계정·Refresh Token DB 모델과 저장소 |
| `security` | JWT 검증, `SecurityContext` 구성, URL 접근 권한 설정 |
| `common/response` | 성공·오류 응답의 공통 외형 |
| `common/exception` | 오류 코드, 예외 타입, 예외를 HTTP 응답으로 변환 |

### 현재 구현의 큰 흐름

1. `AuthController`가 요청을 받고 입력 DTO와 쿠키를 서비스에 전달한다.
2. `AuthService`가 state 검증, provider 선택, Kakao 사용자 조회, 계정 생성·조회, 토큰 발급을 조합한다.
3. provider 통신은 `OAuthProviderClient`, state 저장은 `OAuthStateStore`, Access Token 발급은 `AccessTokenIssuer` 인터페이스 뒤로 감춰져 있다.
4. 실제 구현은 각각 `KakaoOAuthProviderClient`, `RdbOAuthStateStore`, `JwtAccessTokenIssuer`다.
5. 사용자와 토큰은 JPA Entity와 Repository를 통해 DB에 저장한다.
6. 오류는 `ApiException` 또는 전역 예외 처리기를 거쳐 `ApiResponse`의 `error` 영역으로 내려간다.

## 2. 애플리케이션·설정 파일

| 파일 | 타입 | 메소드/역할 |
| --- | --- | --- |
| [`KtbAgileBackendApplication.java`](../src/main/java/com/example/KTB_Agile_backend/KtbAgileBackendApplication.java) | Spring Boot 진입 클래스 | `main(String[] args)`: `SpringApplication.run`으로 애플리케이션을 시작한다. |
| [`application.yaml`](../src/main/resources/application.yaml) | 런타임 설정 | 애플리케이션 이름, JWT secret, Access Token TTL, Refresh Token TTL, OAuth state TTL, 쿠키 Secure 여부, callback 이후 프론트엔드 redirect URI, Kakao client 설정을 환경 변수로 받는다. |
| [`test application.yaml`](../src/test/resources/application.yaml) | 테스트 설정 | 테스트용 CORS origin, JWT·Kakao 값, TTL, 로컬 쿠키 설정, 프론트엔드 redirect URI를 제공한다. |
| [`build.gradle`](../build.gradle) | Gradle 빌드 설정 | Spring Web MVC, Validation, Security, OAuth2 JOSE, Spring Data JPA, H2/MySQL, Lombok 의존성과 테스트 실행을 설정한다. |
| [`settings.gradle`](../settings.gradle) | Gradle 프로젝트 설정 | 루트 프로젝트 이름을 `KTB-Agile-backend`로 지정한다. |
| [`Dockerfile`](../Dockerfile) | 컨테이너 빌드 설정 | Gradle로 layered boot jar를 만들고, non-root 사용자로 8080 포트에서 실행한다. |

기본 TTL은 다음과 같다.

| 설정 키 | 기본값 | 사용처 |
| --- | ---: | --- |
| `auth.jwt.access-token-ttl-seconds` | `900`초 | JWT `exp`와 응답 `expiresIn` |
| `auth.refresh-token-ttl-days` | `14`일 | Refresh Token DB 만료 시각과 쿠키 `Max-Age` |
| `auth.oauth.state-ttl-seconds` | `300`초 | OAuth state DB 만료 시각, state 응답, state 쿠키 |
| `auth.cookie.secure` | `false` | 세 인증 쿠키의 `Secure` 속성. 운영 HTTPS에서는 `true`로 설정 |
| `auth.oauth.frontend-redirect-uri` | `http://127.0.0.1:3000` | Kakao callback 성공 후 `302 Location` 대상 |

## 3. `auth` 영역

### 3.1 Controller

파일: [`AuthController.java`](../src/main/java/com/example/KTB_Agile_backend/auth/controller/AuthController.java)

`/auth`를 공통 경로로 사용하는 인증 API Controller다. `oauth_state`, `refresh_token` 쿠키의 생성·삭제도 담당한다.

| 메소드 | 역할 |
| --- | --- |
| `AuthController(AuthService, long, long, boolean, String)` | 주입받은 TTL을 `Duration`으로 변환하고 쿠키 Secure 여부와 callback 이후 프론트엔드 redirect URI를 보관한다. |
| `issueOAuthState()` | `AuthService.issueOAuthState()`를 호출하고 `OAuthStateResponse`를 JSON으로 반환한다. 동시에 `oauth_state` HttpOnly 쿠키를 설정한다. |
| `kakaoCallback(String, String, String)` | Kakao가 redirect한 `code`, `state`, `oauth_state` 쿠키를 받아 공통 로그인 흐름을 실행한다. 성공 시 Refresh Token 쿠키와 `302 Location`을 반환해 `auth.oauth.frontend-redirect-uri`로 보낸다. |
| `oauthLogin(OAuthLoginRequest, String)` | 요청 DTO와 `oauth_state` 쿠키를 `AuthService.oauthLoginWithTokens`에 전달한다. 신규 사용자는 `201 Created`, 기존 사용자는 `200 OK`로 응답하고 Refresh Token을 쿠키에 설정한다. |
| `reissueToken(String)` | `refresh_token` 쿠키를 `AuthService.reissueToken`에 전달하고 `TokenReissueResponse`를 반환한다. |
| `logout(String)` | `refresh_token` 쿠키를 `AuthService.logout`에 전달한 뒤 Refresh Token 쿠키를 삭제하고 `204 No Content`를 반환한다. 실제 URL 접근에는 SecurityConfig의 Access Token 인증도 필요하다. |
| `stateCookie(String)` | state 쿠키를 `HttpOnly`, 설정된 `Secure`, `SameSite=Lax`, `Path=/auth`, 설정된 TTL로 만든다. |
| `refreshTokenCookie(String)` | Refresh Token 쿠키를 `HttpOnly`, 설정된 `Secure`, `SameSite=Lax`, `Path=/auth`, 14일 TTL로 만든다. |
| `deleteRefreshTokenCookie()` | 같은 이름·경로의 Refresh Token 쿠키를 빈 값과 `Max-Age=0`으로 만든다. |

### 3.2 인증 서비스

#### `AuthService`

파일: [`AuthService.java`](../src/main/java/com/example/KTB_Agile_backend/auth/service/AuthService.java)

인증 관련 여러 컴포넌트를 하나의 유스케이스로 조합한다.

| 메소드 | 역할 |
| --- | --- |
| `issueOAuthState()` | 현재 기본 provider인 `KAKAO`를 사용해 `OAuthStateService.issue`를 호출한다. |
| `oauthLoginWithTokens(OAuthLoginRequest, String)` | provider 정규화 → state 검증·소비 → provider 사용자 조회 → 사용자 계정 생성·조회 → 활성 상태 확인 → Access/Refresh Token 발급 → `AuthTokenResult` 생성 순서를 실행한다. |
| `reissueToken(String)` | Refresh Token을 검증해 사용자를 얻고, 활성 사용자인지 확인한 뒤 새 Access Token만 발급한다. |
| `logout(String)` | `RefreshTokenService.revoke`에 Refresh Token 폐기를 위임한다. |
| `findProviderClient(String)` | 주입된 `OAuthProviderClient` 목록에서 provider 이름이 일치하는 client를 찾는다. 없으면 `AUTH_PROVIDER_UNSUPPORTED`를 발생시킨다. |
| `validateUserInfo(OAuthUserInfo, String)` | provider, provider 사용자 ID, nickname이 정상인지 확인한다. 실패하면 `AUTHENTICATION_FAILED`를 발생시킨다. |
| `ensureActive(User)` | 사용자 상태가 `ACTIVE`이고 `deletedAt`이 없는지 확인한다. |
| `normalizeProvider(String)` | null·공백을 거부하고 앞뒤 공백 제거 후 대문자로 변환한다. |
| `authenticationFailed()` | OAuth 인증 실패용 `ApiException`을 만든다. |

#### `OAuthStateService`

파일: [`OAuthStateService.java`](../src/main/java/com/example/KTB_Agile_backend/auth/service/OAuthStateService.java)

OAuth state를 생성하고, 요청 state와 쿠키 state를 비교한 뒤 서버 저장소에서 한 번만 소비한다.

| 메소드 | 역할 |
| --- | --- |
| `OAuthStateService(OAuthStateStore, long)` | state 저장소와 TTL을 주입받는다. |
| `issue(String)` | 32바이트 난수를 URL-safe Base64 문자열로 만들고, SHA-256 해시·provider·만료 시각을 저장한 뒤 원본 state를 반환한다. |
| `consume(String, String, String)` | 쿠키와 요청 state가 같은지 constant-time 비교를 하고, state 해시를 저장소에 전달해 만료되지 않고 미소비 상태인지 원자적으로 소비한다. |
| `normalizeProvider(String)` | provider를 검증하고 대문자로 정규화한다. |
| `authenticationFailed()` | state 검증 실패용 `AUTH_OAUTH_STATE_INVALID` 예외를 만든다. |

#### `RefreshTokenService`

파일: [`RefreshTokenService.java`](../src/main/java/com/example/KTB_Agile_backend/auth/service/RefreshTokenService.java)

원본 Refresh Token은 반환·저장하고, DB에는 해시만 저장한다.

| 메소드 | 역할 |
| --- | --- |
| `RefreshTokenService(RefreshTokenRepository, long)` | Refresh Token TTL을 주입받는다. |
| `issue(User)` | 32바이트 난수 토큰을 발급하고 SHA-256 해시와 만료 시각을 `RefreshToken`으로 저장한 뒤 원본 토큰을 반환한다. |
| `requireValidUser(String)` | 입력 토큰을 해시해 미폐기 DB 레코드를 찾고 만료 여부를 확인한 뒤 연결된 사용자를 반환한다. 만료 토큰은 폐기 처리 후 예외를 발생시킨다. |
| `revoke(String)` | 토큰이 없으면 아무 작업도 하지 않고, 있으면 해시로 찾아 `deletedAt`을 기록한다. |
| `invalidRefreshToken()` | Refresh Token 검증 실패용 `AUTH_REFRESH_TOKEN_INVALID` 예외를 만든다. |

#### 보조 타입

| 파일 | 역할 및 메소드 |
| --- | --- |
| [`AuthTokenResult.java`](../src/main/java/com/example/KTB_Agile_backend/auth/service/AuthTokenResult.java) | `AuthResponse response`, 원본 `refreshToken`을 함께 전달하는 내부 `record`다. `response()`, `refreshToken()`은 record가 생성한다. Controller가 Refresh Token을 쿠키로 옮긴 후 JSON에는 노출하지 않는다. |
| [`Hashing.java`](../src/main/java/com/example/KTB_Agile_backend/auth/service/Hashing.java) | `sha256(String)`: UTF-8 문자열의 SHA-256 해시를 64자리 hex 문자열로 반환한다. 인스턴스 생성자는 막혀 있다. |

### 3.3 Provider client

#### `OAuthProviderClient`

파일: [`OAuthProviderClient.java`](../src/main/java/com/example/KTB_Agile_backend/auth/client/OAuthProviderClient.java)

OAuth provider별 구현을 같은 방식으로 호출하기 위한 인터페이스다.

| 메소드 | 역할 |
| --- | --- |
| `provider()` | 구현체가 담당하는 provider 이름을 반환한다. |
| `getUserInfo(String authorizationCode)` | 인가 코드를 provider 사용자 정보로 교환한다. |

#### `KakaoOAuthProviderClient`

파일: [`KakaoOAuthProviderClient.java`](../src/main/java/com/example/KTB_Agile_backend/auth/client/KakaoOAuthProviderClient.java)

`OAuthProviderClient`의 Kakao 구현이다. 현재 주입되는 provider client는 이 구현체 하나다.

| 메소드 | 역할 |
| --- | --- |
| 생성자 | `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI` 설정을 받는다. |
| `provider()` | `KAKAO`를 반환한다. |
| `getUserInfo(String)` | Kakao token API에 인가 코드를 보내 `KakaoTokenResponse`를 받고, 받은 access token으로 Kakao user API를 호출한다. 응답을 `OAuthUserInfo`로 변환한다. |
| `firstNonBlank(String...)` | 첫 번째 non-blank 값을 찾는다. nickname이 없으면 `kakao-{providerUserId}`를 사용한다. |
| `authenticationFailed(Throwable)` | Kakao API 응답 오류를 `AUTH_OAUTH_AUTHENTICATION_FAILED`로 감싼다. |

호출하는 외부 API는 다음과 같다.

| 외부 API | 방식 | 사용 DTO |
| --- | --- | --- |
| `https://kauth.kakao.com/oauth/token` | `POST`, form-urlencoded | 응답을 `KakaoTokenResponse`로 매핑 |
| `https://kapi.kakao.com/v2/user/me` | `GET`, `Authorization: Bearer {Kakao access token}` | 응답을 `KakaoUserResponse`로 매핑 |

client secret과 redirect URI는 공백이 아닐 때만 token 요청 form에 포함한다. Kakao의 access token은 서버 내부에서만 사용하며 애플리케이션 응답으로 반환하지 않는다.

### 3.4 OAuth state 저장

| 파일 | 타입 | 메소드/역할 |
| --- | --- | --- |
| [`OAuthStateStore.java`](../src/main/java/com/example/KTB_Agile_backend/auth/state/OAuthStateStore.java) | 인터페이스 | `save(String, String, Instant)`: state 해시를 저장한다. `consumeIfValid(String, String, Instant)`: 유효한 state를 한 번 소비하고 성공 여부를 반환한다. |
| [`RdbOAuthStateStore.java`](../src/main/java/com/example/KTB_Agile_backend/auth/state/RdbOAuthStateStore.java) | 구현 클래스 | `save`: `OAuthState`를 Repository에 저장한다. `consumeIfValid`: Repository의 update 결과가 1인지 boolean으로 변환한다. |
| [`OAuthState.java`](../src/main/java/com/example/KTB_Agile_backend/auth/entity/OAuthState.java) | JPA Entity | `oauth_states` 테이블에 state 해시, provider, 만료 시각, 생성 시각, 소비 시각을 저장한다. `OAuthState(String, String, Instant)` 생성자로 새 state를 만든다. |
| [`OAuthStateRepository.java`](../src/main/java/com/example/KTB_Agile_backend/auth/repository/OAuthStateRepository.java) | Spring Data Repository | `consumeIfValid(String, String, Instant, Instant)`: state 해시·provider가 일치하고, 만료 전이며, `consumedAt`이 null인 row를 조건부 update한다. 반환값은 변경된 row 수다. |

state 원본은 DB에 저장하지 않고 SHA-256 해시만 저장한다. `consumedAt is null` 조건을 포함한 update라서 같은 state를 두 번 사용할 수 없다.

### 3.5 Access Token 발급

| 파일 | 타입 | 메소드/역할 |
| --- | --- | --- |
| [`AccessTokenIssuer.java`](../src/main/java/com/example/KTB_Agile_backend/auth/token/AccessTokenIssuer.java) | 인터페이스 | `issue(User)`: Access Token 발급. `expiresInSeconds()`: 설정된 유효 시간을 반환한다. |
| [`JwtAccessTokenIssuer.java`](../src/main/java/com/example/KTB_Agile_backend/auth/token/JwtAccessTokenIssuer.java) | 구현 클래스 | `issue(User)`: HS256 JWT를 발급하고 `sub=user.id`, `role=user.userRole`, `iat`, `exp`를 넣는다. `expiresInSeconds()`: Access Token TTL을 반환한다. JWT secret이 32바이트보다 짧으면 생성자를 실패시킨다. |

## 4. `auth.dto` 구성

DTO는 외부 provider 응답, 애플리케이션 내부 중간값, API 요청·응답을 분리한다.

### 요청 DTO

파일: [`OAuthLoginRequest.java`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/request/OAuthLoginRequest.java)

| 필드 | 타입 | 검증/역할 |
| --- | --- | --- |
| `provider` | `String` | `@NotBlank`; provider 선택값. 현재 `KAKAO`만 지원한다. |
| `authorizationCode` | `String` | `@NotBlank`; Kakao authorization code다. |
| `state` | `String` | `@NotBlank`; OAuth state 응답과 함께 받은 값을 보낸다. |

### 내부·provider DTO

| 파일 | 타입/필드 | 역할 |
| --- | --- | --- |
| [`OAuthUserInfo.java`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/OAuthUserInfo.java) | `record(provider, providerUserId, nickname, profileImageUrl)` | Kakao 전용 응답을 인증 서비스가 이해하는 공통 사용자 정보로 바꾼 내부 DTO다. |
| [`KakaoTokenResponse.java`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/provider/KakaoTokenResponse.java) | `accessToken`; JSON `access_token` | Kakao token API 응답에서 provider access token만 받는다. |
| [`KakaoUserResponse.java`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/provider/KakaoUserResponse.java) | `id`, `kakaoAccount` | Kakao user API 응답의 사용자 ID와 계정 정보를 받는다. |
| `KakaoUserResponse.KakaoAccount` | `profile` | Kakao 계정의 프로필 영역이다. |
| `KakaoUserResponse.Profile` | `nickname`, `profileImageUrl`; JSON `profile_image_url` | 로그인에 사용할 nickname과 프로필 이미지 URL이다. |

### 응답 DTO

| 파일 | 필드 | 역할 |
| --- | --- | --- |
| [`OAuthStateResponse.java`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/response/OAuthStateResponse.java) | `state`, `expiresIn` | 로그인 시작 API가 내려주는 state와 만료 시간(초)이다. |
| [`AuthResponse.java`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/response/AuthResponse.java) | `accessToken`, `tokenType`, `expiresIn`, `isNewUser`, `user` | `POST /auth/oauth` 성공 시 JSON body에 내려가는 Access Token과 사용자 요약 정보다. Kakao callback은 이 값을 body에 넣지 않고 302 redirect 후 `/auth/refresh`를 사용한다. |
| [`UserProfile.java`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/response/UserProfile.java) | `userId`, `nickname`, `profileImageUrl` | `AuthResponse.user`에 들어가는 사용자 요약 정보다. |
| [`TokenReissueResponse.java`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/response/TokenReissueResponse.java) | `accessToken`, `tokenType`, `expiresIn`, `needsPreferenceSetup` | Refresh Token으로 재발급한 새 Access Token과 거래 취향 설정 필요 여부를 반환한다. |

`RefreshTokenResponse`, `TokenReissueRequest`, `LogoutRequest`, `LogoutResponse`는 현재 없다. Refresh Token은 요청·응답 JSON이 아니라 쿠키로 처리하고, 로그아웃은 본문 없이 `204`를 반환하기 때문이다.

## 5. `user` 영역

### 5.1 Entity

| 파일 | 주요 필드 | 메소드/역할 |
| --- | --- | --- |
| [`User.java`](../src/main/java/com/example/KTB_Agile_backend/user/entity/User.java) | `id`, `profileImageUrl`, `nickname`, `userRole`, `createdAt`, `updatedAt`, `deletedAt`, `userStatus` | `User(String)`, `User(String, String)`: 사용자 생성. `withdraw(LocalDateTime)`: 탈퇴 시각을 기록하고 상태를 `WITHDRAWN`으로 바꾼다. 기본 role은 `USER`, 기본 status는 `ACTIVE`다. |
| [`SocialAccount.java`](../src/main/java/com/example/KTB_Agile_backend/user/entity/SocialAccount.java) | `id`, `user`, `provider`, `providerUserId`, `createdAt`, `lastLoginAt` | `SocialAccount(User, String, String)`: 소셜 계정 연결. `createdAt`은 연결 시각으로 사용하고, `recordLogin()`은 마지막 로그인 시각을 현재 시각으로 갱신한다. `(provider, providerUserId)` 유니크 제약이 있다. |
| [`RefreshToken.java`](../src/main/java/com/example/KTB_Agile_backend/user/entity/RefreshToken.java) | `id`, `user`, `tokenHash`, `expiresAt`, `createdAt`, `deletedAt` | `RefreshToken(User, String, LocalDateTime)`: 해시 토큰을 생성. `revoke()`: `deletedAt`을 기록해 폐기한다. |
| [`UserRole.java`](../src/main/java/com/example/KTB_Agile_backend/user/entity/UserRole.java) | `USER`, `ADMIN` | JWT role과 Spring Security authority로 사용할 역할 enum이다. |
| [`UserStatus.java`](../src/main/java/com/example/KTB_Agile_backend/user/entity/UserStatus.java) | `ACTIVE`, `INACTIVE`, `WITHDRAWN` | 사용자 상태 enum이다. 로그인·토큰 재발급은 `ACTIVE`만 허용한다. |

### 5.2 Service

#### `AccountProvisioningService`

파일: [`AccountProvisioningService.java`](../src/main/java/com/example/KTB_Agile_backend/user/service/AccountProvisioningService.java)

OAuth provider 사용자와 내부 `User`/`SocialAccount`를 연결한다.

| 메소드 | 역할 |
| --- | --- |
| `findOrCreate(OAuthUserInfo)` | provider와 provider 사용자 ID로 기존 `SocialAccount`을 찾는다. 기존 계정이면 활성 사용자를 반환하고 `lastLoginAt`을 갱신한다. 없으면 `User`와 `SocialAccount`을 생성해 `AccountResult`로 반환한다. |
| `normalizeProvider(String)` | provider를 검증하고 대문자로 정규화한다. |
| `authenticationFailed()` | 연결된 사용자가 비활성일 때 인증 실패 예외를 만든다. |

새 소셜 계정 저장 중 유니크 제약 위반이 발생하면 `SOCIAL_ACCOUNT_CONFLICT`(409)로 변환한다.

#### `AccountWithdrawalService`

파일: [`AccountWithdrawalService.java`](../src/main/java/com/example/KTB_Agile_backend/user/service/AccountWithdrawalService.java)

| 메소드 | 역할 |
| --- | --- |
| `withdraw(Long userId)` | 사용자를 조회해 `User.withdraw`를 실행하고, 같은 사용자의 미폐기 Refresh Token을 모두 폐기한다. |

#### 보조 타입

| 파일 | 역할 |
| --- | --- |
| [`AccountResult.java`](../src/main/java/com/example/KTB_Agile_backend/user/service/AccountResult.java) | `User user`, `boolean newUser`를 담는 내부 `record`다. `user()`, `newUser()` 접근자는 record가 생성한다. |

### 5.3 Controller

파일: [`UserController.java`](../src/main/java/com/example/KTB_Agile_backend/user/controller/UserController.java)

`/users`를 공통 경로로 사용한다.

| 메소드 | 역할 |
| --- | --- |
| `withdraw(Authentication)` | `SecurityContext`의 principal(`Jwt sub`)을 Long 사용자 ID로 바꿔 `AccountWithdrawalService.withdraw`를 호출하고 `204 No Content`를 반환한다. |

### 5.4 Repository

| 파일 | 메소드/역할 |
| --- | --- |
| [`UserRepository.java`](../src/main/java/com/example/KTB_Agile_backend/user/repository/UserRepository.java) | `findByIdAndUserStatusAndDeletedAtIsNull(Long, UserStatus)`: 상태가 일치하고 탈퇴 시각이 없는 사용자를 찾는다. `findActiveById(Long)`: 위 메소드에 `ACTIVE`를 적용한 default 메소드다. |
| [`SocialAccountRepository.java`](../src/main/java/com/example/KTB_Agile_backend/user/repository/SocialAccountRepository.java) | `findByProviderAndProviderUserId(String, String)`: provider 계정 연결을 찾는다. |
| [`RefreshTokenRepository.java`](../src/main/java/com/example/KTB_Agile_backend/user/repository/RefreshTokenRepository.java) | `findByTokenHashAndDeletedAtIsNull(String)`: 미폐기 토큰을 찾는다. `revokeAllByUserId(Long, LocalDateTime)`: 해당 사용자의 모든 미폐기 토큰을 일괄 폐기한다. |

세 Repository는 모두 `JpaRepository`를 상속하므로 `save`, `saveAndFlush`, `findById` 등의 기본 CRUD 메소드도 제공한다.

## 6. `security` 영역

### `JwtAuthenticationFilter`

파일: [`JwtAuthenticationFilter.java`](../src/main/java/com/example/KTB_Agile_backend/security/JwtAuthenticationFilter.java)

| 메소드 | 역할 |
| --- | --- |
| `JwtAuthenticationFilter(JwtDecoder)` | JWT 검증기를 주입받는다. |
| `doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)` | `Authorization: Bearer ...`를 읽고 JWT 서명·시간·claim을 검증한다. 유효하면 `sub`를 principal로, `role`을 `ROLE_USER`/`ROLE_ADMIN` authority로 변환해 `SecurityContext`에 저장한다. 잘못된 토큰은 context를 비우고 다음 필터로 넘긴다. |
| `resolveToken(HttpServletRequest)` | Bearer 접두사를 대소문자 구분 없이 확인하고 토큰 문자열을 추출한다. |

### `SecurityConfig`

파일: [`SecurityConfig.java`](../src/main/java/com/example/KTB_Agile_backend/security/SecurityConfig.java)

| 메소드 | 역할 |
| --- | --- |
| `jwtDecoder(String jwtSecret)` | 동일한 HMAC secret과 HS256 알고리즘으로 JWT Decoder Bean을 만든다. secret이 32바이트보다 짧으면 실패한다. |
| `securityFilterChain(HttpSecurity, JwtDecoder, ObjectMapper)` | CSRF·form login·HTTP Basic을 끄고 stateless 세션을 적용한다. `/auth/oauth/**`, `/auth/kakao/callback`, `/auth/refresh`, `/error`를 공개하고 나머지는 인증을 요구하며 JWT 필터와 오류 처리기를 등록한다. |
| `writeError(HttpServletResponse, ObjectMapper, ErrorCode)` | Security 단계에서 발생한 401/403을 `ErrorCode`의 기본 메시지와 함께 공통 `ApiResponse` JSON으로 작성한다. URI별 메시지를 선택하지 않는다. |

현재 접근 권한은 다음과 같다.

| 경로 | 접근 |
| --- | --- |
| `/auth/oauth/**` | 공개 |
| `/auth/kakao/callback` | 공개. 단, state 쿠키와 query parameter 검증은 Controller·Service에서 수행한다. |
| `/auth/refresh` | 공개. 단, Refresh Token 쿠키 검증은 서비스에서 수행한다. |
| `/error` | 공개 |
| `/auth/logout` | 유효한 Access Token 필요 |
| `/users` 및 그 외 | 유효한 Access Token 필요 |

## 7. 공통 응답·예외

| 파일 | 메소드/역할 |
| --- | --- |
| [`ApiResponse.java`](../src/main/java/com/example/KTB_Agile_backend/common/response/ApiResponse.java) | `record ApiResponse<T>(T data, ErrorResponse error)`. 모든 Controller 성공·실패 body를 `data`와 `error`로 감싼다. |
| [`ErrorResponse.java`](../src/main/java/com/example/KTB_Agile_backend/common/response/ErrorResponse.java) | `code`, `message`, `details`를 담는다. 중첩 `Field` record는 `field`, `reason`을 담는다. |
| [`ErrorCode.java`](../src/main/java/com/example/KTB_Agile_backend/common/exception/ErrorCode.java) | 도메인 의미가 있는 오류 코드와 HTTP status·기본 메시지를 매핑한다. `value()`, `status()`, `message()`로 꺼낸다. |
| [`ErrorDetail.java`](../src/main/java/com/example/KTB_Agile_backend/common/exception/ErrorDetail.java) | 예외 계층에서 사용하는 필드별 오류 정보인 `field`, `reason`을 담는다. |
| [`ApiException.java`](../src/main/java/com/example/KTB_Agile_backend/common/exception/ApiException.java) | 오류 코드·메시지·상세 필드·원인 예외만 보관한다. HTTP 응답 DTO는 생성하지 않는다. |
| [`GlobalExceptionHandler.java`](../src/main/java/com/example/KTB_Agile_backend/common/exception/GlobalExceptionHandler.java) | `ApiException`·검증 오류·본문 형식 오류·예상하지 못한 오류를 공통 `ApiResponse`로 변환한다. 예상하지 못한 오류는 URI와 무관하게 일반 500 메시지를 반환한다. |

공통 오류 body의 기본 형태는 다음과 같다.

```json
{
  "data": null,
  "error": {
    "code": "REQUEST_VALIDATION_FAILED",
    "message": "요청 값이 올바르지 않습니다.",
    "details": []
  }
}
```

## 8. 테스트 파일

| 파일 | 검증 범위 |
| --- | --- |
| [`KtbAgileBackendApplicationTests.java`](../src/test/java/com/example/KTB_Agile_backend/KtbAgileBackendApplicationTests.java) | Spring context 기동과 인증 없는 로그아웃의 표준 401 응답 |
| [`AuthControllerTest.java`](../src/test/java/com/example/KTB_Agile_backend/auth/controller/AuthControllerTest.java) | state 발급, POST 로그인 성공, Kakao callback 성공, 로그아웃 쿠키 삭제, 입력 오류, Refresh Token 오류·500 응답 |
| [`AuthServiceTest.java`](../src/test/java/com/example/KTB_Agile_backend/auth/service/AuthServiceTest.java) | 첫 로그인 계정 생성·토큰 발급, Access Token 재발급, 로그아웃 위임 |
| [`OAuthStateRepositoryTest.java`](../src/test/java/com/example/KTB_Agile_backend/auth/repository/OAuthStateRepositoryTest.java) | OAuth state의 1회 소비 |
| [`JwtAccessTokenIssuerTest.java`](../src/test/java/com/example/KTB_Agile_backend/auth/token/JwtAccessTokenIssuerTest.java) | JWT role·만료 시간과 짧은 secret 거부 |
| [`RefreshTokenServiceTest.java`](../src/test/java/com/example/KTB_Agile_backend/auth/token/RefreshTokenServiceTest.java) | Refresh Token 난수 발급, 해시 저장, 검증, 폐기 |
| [`JwtAuthenticationFilterTest.java`](../src/test/java/com/example/KTB_Agile_backend/security/JwtAuthenticationFilterTest.java) | 유효 JWT의 `SecurityContext` 저장과 잘못된 JWT 무시 |
| [`UserControllerTest.java`](../src/test/java/com/example/KTB_Agile_backend/user/controller/UserControllerTest.java) | 인증된 사용자 ID로 회원 탈퇴 위임 |
| [`UserTests.java`](../src/test/java/com/example/KTB_Agile_backend/user/entity/UserTests.java) | 기본 role/status와 탈퇴 상태 변경 |
| [`SocialAccountRepositoryTest.java`](../src/test/java/com/example/KTB_Agile_backend/user/repository/SocialAccountRepositoryTest.java) | provider 계정 중복 유니크 제약 |
| [`AccountProvisioningServiceTest.java`](../src/test/java/com/example/KTB_Agile_backend/user/service/AccountProvisioningServiceTest.java) | 기존 계정, 비활성 계정, 신규 계정, 동시 연결 충돌 |
| [`AccountWithdrawalServiceTest.java`](../src/test/java/com/example/KTB_Agile_backend/user/service/AccountWithdrawalServiceTest.java) | 사용자 탈퇴와 Refresh Token 일괄 폐기 |

## 9. 관련 문서

- [`auth-dto-scenarios.md`](./auth-dto-scenarios.md): 인증 DTO를 중심으로 정리한 기존 시나리오 문서
- [`adr/0001-oauth-state-storage.md`](./adr/0001-oauth-state-storage.md): OAuth state 저장 방식 결정
- [`adr/0002-authorization-security-context.md`](./adr/0002-authorization-security-context.md): JWT와 `SecurityContext` 처리 결정
- [`kakao-login-flow.md`](./kakao-login-flow.md): Kakao 로그인 요청·호출·응답의 상세 순서
