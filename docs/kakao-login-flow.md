# Kakao 로그인 호출·DTO·응답 흐름

이 문서는 현재 구현을 기준으로 Kakao 로그인 시작부터 로그인 성공, 실패, 토큰 재발급·로그아웃까지의 실제 호출 순서를 설명한다.

## 1. 먼저 알아둘 현재 구조

- Kakao Redirect URI는 백엔드의 `/auth/kakao/callback`으로 설정한다. Kakao가 이 endpoint로 `code`와 `state`를 redirect하면 백엔드가 로그인 처리를 이어간다.
- callback 성공 후 백엔드가 `FRONTEND_REDIRECT_URI`로 `302 Found` redirect하고 Refresh Token 쿠키를 반환한다. 프론트엔드는 이동 후 `/auth/refresh`를 호출해 Access Token을 받는다.
- 로그인 시작 시 provider는 `AuthService`에서 `KAKAO`로 고정해 state를 발급한다.
- Access Token은 POST 로그인·토큰 재발급의 JSON body로 반환한다. callback은 프론트엔드로 redirect한 뒤 `/auth/refresh`로 Access Token을 받는다.
- Refresh Token은 JSON body에 포함하지 않고 `HttpOnly` 쿠키로만 반환한다.
- Kakao access token은 provider API 호출에만 사용하고 애플리케이션 응답으로 전달하지 않는다.
- `AuthTokenResult`는 Controller와 Service 사이의 내부 전달 객체라 Jackson 응답 DTO가 아니다.
- 실제 보안 설정에서 `/auth/logout`은 공개 경로가 아니므로 유효한 Access Token이 있어야 Controller까지 도달한다.

## 2. 전체 순서

```text
클라이언트
  │
  ├─ 1. GET /auth/oauth/state
  │       └─ AuthController.issueOAuthState
  │            └─ AuthService.issueOAuthState
  │                 └─ OAuthStateService.issue("KAKAO")
  │                      ├─ state 원본 생성
  │                      └─ SHA-256(state) 저장
  │
  ├─ 2. state로 Kakao 인증 화면 이동
  │       └─ Kakao가 백엔드 `/auth/kakao/callback`으로 code와 state를 redirect
  │
  ├─ 3-A. GET /auth/kakao/callback?code=...&state=...
  │       └─ AuthController.kakaoCallback
  │            └─ AuthService.oauthLoginWithTokens
  │                 └─ 302 Location: FRONTEND_REDIRECT_URI
  │                      └─ 프론트엔드 POST /auth/refresh
  │
  └─ 3-B. POST /auth/oauth (클라이언트 직접 전달 방식)
          └─ AuthController.oauthLogin
               └─ AuthService.oauthLoginWithTokens
                    ├─ OAuthStateService.consume
                    ├─ KakaoOAuthProviderClient.getUserInfo
                    │    ├─ POST Kakao token API
                    │    └─ GET Kakao user API
                    ├─ AccountProvisioningService.findOrCreate
                    ├─ JwtAccessTokenIssuer.issue
                    ├─ RefreshTokenService.issue
                    └─ AuthTokenResult
                         └─ AuthResponse는 body
                            Refresh Token은 Set-Cookie
```

state 검증이 실패하면 provider API를 호출하지 않는다. 반대로 state가 먼저 소비되므로, state 검증 뒤 Kakao API나 계정 생성에서 실패하면 같은 state로 재시도할 수 없다.

## 3. 단계별 상세 호출

### 3.1 로그인 시작: state 발급

#### 요청

```http
GET /auth/oauth/state
```

`SecurityConfig`에서 `/auth/oauth/**`와 `/auth/kakao/callback`을 공개했으므로 Access Token이 없어도 된다.

#### 호출 순서

| 순서 | 호출 주체 | 호출 메소드 | 결과 |
| ---: | --- | --- | --- |
| 1 | HTTP | `AuthController.issueOAuthState()` | state 발급 API 진입 |
| 2 | Controller | `AuthService.issueOAuthState()` | 기본 provider `KAKAO` 전달 |
| 3 | Service | `OAuthStateService.issue("KAKAO")` | 32바이트 난수 state 생성 |
| 4 | State service | `Hashing.sha256(state)` | 원본 state의 해시 생성 |
| 5 | State service | `OAuthStateStore.save(hash, "KAKAO", expiresAt)` | RDB 구현이 `OAuthState`를 저장 |
| 6 | Controller | `stateCookie(state)` | 원본 state를 쿠키에 저장 |

#### 성공 응답

상태 코드는 `200 OK`다.

```http
Set-Cookie: oauth_state={원본 state}; Path=/auth; Max-Age=300; Expires={...}; HttpOnly; SameSite=Lax
Content-Type: application/json
```

로컬 HTTP에서는 `COOKIE_SECURE=false`로 `Secure`를 생략하고, 운영 HTTPS에서는 `COOKIE_SECURE=true`로 설정한다.

```json
{
  "data": {
    "state": "{원본 state}",
    "expiresIn": 300
  },
  "error": null
}
```

응답의 `state`와 쿠키의 `oauth_state`는 같은 원본 값이다. DB에는 이 원본이 아니라 SHA-256 해시가 저장된다.

### 3.2 Kakao에서 authorization code 받기

Kakao 콘솔의 Redirect URI와 Spring 환경변수 `KAKAO_REDIRECT_URI`는 백엔드 callback 주소와 정확히 같아야 한다.

로컬 예시는 다음과 같다.

```text
http://127.0.0.1:8080/auth/kakao/callback
```

Kakao 인증이 끝나면 Kakao가 위 주소로 브라우저를 redirect하며, query parameter로 `code`와 `state`를 전달한다. 백엔드는 Kakao 로그인 화면으로 redirect를 시작하지 않고, state 발급 API만 제공한다.

### 3.3 Kakao callback 로그인: `GET /auth/kakao/callback`

#### 요청

```http
GET /auth/kakao/callback?code={Kakao authorization code}&state={state}
Cookie: oauth_state={state}
```

`AuthController.kakaoCallback`은 query parameter의 `code`, `state`와 `oauth_state` 쿠키를 받아 `AuthService.oauthLoginWithTokens`에 전달한다. 이후 state 검증, Kakao API 호출, 계정 처리, 애플리케이션 토큰 발급은 `POST /auth/oauth`와 같은 흐름이다.

callback 성공 응답은 `302 Found`이며, `Location` 헤더는 `FRONTEND_REDIRECT_URI`를 가리킨다. Refresh Token은 `Set-Cookie`로 전달되고 응답 body에는 Access Token을 넣지 않는다. 프론트엔드는 redirect 후 기존 `POST /auth/refresh`를 호출해 Access Token을 받는다.

### 3.4 로그인 요청: `POST /auth/oauth`

프론트엔드가 Kakao callback을 직접 받은 뒤 authorization code를 백엔드에 전달하는 대체 방식이다. Kakao Redirect URI를 백엔드로 설정하는 현재 기본 흐름에서는 사용할 필요가 없지만, Postman 등에서 직접 테스트할 때 사용할 수 있다.

#### 요청

```http
POST /auth/oauth
Content-Type: application/json
Cookie: oauth_state={state}
```

```json
{
  "provider": "KAKAO",
  "authorizationCode": "{Kakao authorization code}",
  "state": "{같은 state}"
}
```

요청 body는 [`OAuthLoginRequest`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/request/OAuthLoginRequest.java)로 역직렬화된다.

| 입력 | 전달 경로 | 받는 코드 |
| --- | --- | --- |
| `provider` | JSON body | `OAuthLoginRequest.provider()` |
| `authorizationCode` | JSON body | `OAuthLoginRequest.authorizationCode()` |
| `state` | JSON body | `OAuthLoginRequest.state()` |
| `oauth_state` | Cookie | `@CookieValue`의 `stateCookie` 인자 |

#### 호출 순서

| 순서 | 호출 주체 | 호출 메소드 | 반환/다음 단계 |
| ---: | --- | --- | --- |
| 1 | Spring MVC | `@Valid @RequestBody OAuthLoginRequest` | 세 필드 `@NotBlank` 검증 |
| 2 | Controller | `AuthService.oauthLoginWithTokens(request, stateCookie)` | 인증 유스케이스 시작 |
| 3 | Auth service | `normalizeProvider(request.provider())` | `kakao`도 `KAKAO`로 변환 |
| 4 | Auth service | `OAuthStateService.consume(stateCookie, request.state(), "KAKAO")` | 쿠키·body state 비교 및 1회 소비 |
| 5 | Auth service | `findProviderClient("KAKAO")` | `KakaoOAuthProviderClient` 선택 |
| 6 | Provider client | `getUserInfo(request.authorizationCode())` | Kakao 사용자 정보를 `OAuthUserInfo`로 변환 |
| 7 | Auth service | `validateUserInfo(userInfo, "KAKAO")` | 필수 provider 정보 확인 |
| 8 | Auth service | `AccountProvisioningService.findOrCreate(userInfo)` | 기존/신규 내부 계정 결정 |
| 9 | Auth service | `ensureActive(user)` | `ACTIVE` 사용자만 통과 |
| 10 | Auth service | `AccessTokenIssuer.issue(user)` | JWT Access Token 발급 |
| 11 | Auth service | `RefreshTokenService.issue(user)` | 원본 Refresh Token 발급 및 해시 저장 |
| 12 | Auth service | `new AuthResponse(...)` | body용 응답 DTO 구성 |
| 13 | Controller | `new AuthTokenResult(response, refreshToken)` 결과 수신 | 내부 Refresh Token과 body DTO 분리 |
| 14 | Controller | `refreshTokenCookie(result.refreshToken())` | Refresh Token을 Set-Cookie로 설정 |

## 4. state 검증 내부 동작

`OAuthStateService.consume`는 다음 순서로 동작한다.

1. 쿠키 값이나 요청 body의 `state`가 null이면 실패한다.
2. `MessageDigest.isEqual`로 두 원본 값을 비교한다.
3. 요청 provider를 정규화한다.
4. 쿠키 state를 SHA-256으로 해시한다.
5. `OAuthStateStore.consumeIfValid`를 호출한다.
6. `OAuthStateRepository.consumeIfValid`가 다음 조건을 모두 만족하는 row만 update한다.
   - `stateHash` 일치
   - `provider` 일치
   - `expiresAt > now`
   - `consumedAt is null`
7. update 결과가 1이 아니면 인증 실패다.

따라서 다음은 모두 같은 `UNAUTHORIZED` 인증 실패 경로다.

- `oauth_state` 쿠키 누락
- body의 `state` 누락 또는 쿠키와 불일치
- 만료된 state
- 이미 사용한 state
- 다른 provider로 발급된 state

## 5. Kakao provider 호출과 DTO 변환

### 5.1 인가 코드 → Kakao access token

`KakaoOAuthProviderClient.getUserInfo`가 다음 form으로 Kakao token API를 호출한다.

```text
POST https://kauth.kakao.com/oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
client_id={KAKAO_CLIENT_ID}
code={authorizationCode}
client_secret={KAKAO_CLIENT_SECRET}   # 공백이 아니면 포함
redirect_uri={KAKAO_REDIRECT_URI}     # 공백이 아니면 포함
```

응답은 [`KakaoTokenResponse`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/provider/KakaoTokenResponse.java)로 받는다.

```json
{
  "access_token": "{Kakao provider access token}"
}
```

Java에서는 `@JsonProperty("access_token")`로 `accessToken()`에 매핑한다. access token이 없으면 서버 내부 오류로 처리된다.

### 5.2 Kakao access token → 사용자 정보

```http
GET https://kapi.kakao.com/v2/user/me
Authorization: Bearer {Kakao provider access token}
```

응답은 [`KakaoUserResponse`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/provider/KakaoUserResponse.java)로 받는다.

```json
{
  "id": 123456789,
  "kakao_account": {
    "profile": {
      "nickname": "사용자",
      "profile_image_url": "https://..."
    }
  }
}
```

다음과 같이 [`OAuthUserInfo`](../src/main/java/com/example/KTB_Agile_backend/auth/dto/OAuthUserInfo.java)로 변환한다.

| Kakao 값 | `OAuthUserInfo` 값 | 비고 |
| --- | --- | --- |
| 고정값 | `provider = "KAKAO"` | provider client가 지정 |
| `id` | `providerUserId = String.valueOf(id)` | 필수 |
| `kakao_account.profile.nickname` | `nickname` | 없으면 `kakao-{id}` fallback |
| `kakao_account.profile.profile_image_url` | `profileImageUrl` | 없으면 null 가능 |

`providerUserId` 또는 nickname이 유효하지 않으면 `AuthService.validateUserInfo`가 인증 실패로 처리한다.

## 6. 내부 계정 생성·조회

`AccountProvisioningService.findOrCreate`는 provider 사용자 ID를 내부 사용자와 연결한다.

### 기존 소셜 계정

```text
SocialAccountRepository.findByProviderAndProviderUserId("KAKAO", providerUserId)
  └─ 존재
      └─ UserRepository.findActiveById(userId)
           ├─ 성공: SocialAccount.recordLogin() -> AccountResult(user, false)
           └─ 실패: UNAUTHORIZED
```

기존 사용자의 nickname과 profile image는 이 로그인 흐름에서 갱신하지 않고, 마지막 로그인 시각만 갱신한다.

### 신규 소셜 계정

```text
SocialAccountRepository.findByProviderAndProviderUserId(...)
  └─ 없음
      ├─ UserRepository.save(new User(nickname, profileImageUrl))
      │    └─ 기본 role USER, status ACTIVE
      └─ SocialAccountRepository.saveAndFlush(new SocialAccount(...))
           └─ AccountResult(user, true)
```

동일 Kakao 계정이 동시에 다른 사용자에 연결되면 DB 유니크 제약 위반을 `SOCIAL_ACCOUNT_CONFLICT`(HTTP 409)로 변환한다.

## 7. 로그인 성공 응답

### 토큰 생성

#### Access Token

`JwtAccessTokenIssuer.issue(user)`가 HS256 JWT를 발급한다.

| claim | 값 |
| --- | --- |
| `sub` | 내부 `User.id` 문자열 |
| `role` | `User.userRole.name()` (`USER` 또는 `ADMIN`) |
| `iat` | 발급 시각 |
| `exp` | 발급 시각 + 기본 900초 |

#### Refresh Token

`RefreshTokenService.issue(user)`가 32바이트 난수를 생성한다.

- 원본 토큰: Controller가 쿠키에만 넣는다.
- DB 저장값: `Hashing.sha256(token)`만 저장한다.
- 만료: 기본 14일.
- 쿠키: `HttpOnly`, 설정된 `Secure`, `SameSite=Lax`, `Path=/auth`.

### 신규 사용자 성공

`AccountResult.newUser()`가 true이면 Controller가 `201 Created`를 선택한다.

```http
HTTP/1.1 201 Created
Set-Cookie: refresh_token={원본 Refresh Token}; Path=/auth; Max-Age=1209600; HttpOnly; SameSite=Lax
Content-Type: application/json
```

```json
{
  "data": {
    "accessToken": "{애플리케이션 JWT}",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "isNewUser": true,
    "user": {
      "userId": 1,
      "nickname": "사용자",
      "profileImageUrl": "https://..."
    }
  },
  "error": null
}
```

### 기존 사용자 성공

기존 소셜 계정이면 body 구조는 같고 `isNewUser`가 false이며 상태 코드는 `200 OK`다. 기존 로그인에서도 Refresh Token을 새로 발급해 `refresh_token` 쿠키를 갱신한다.

## 8. 응답에 오지 않는 값

| 값 | 이유 |
| --- | --- |
| Kakao provider access token | Kakao API 호출 내부에서만 사용한다. |
| 원본 Refresh Token JSON 필드 | `AuthResponse`에 필드가 없고 `AuthTokenResult`는 내부 wrapper다. `Set-Cookie` header에만 있다. |
| `OAuthState` DB row | state 발급 API는 state 문자열과 TTL만 반환한다. DB id·해시는 반환하지 않는다. |
| `KakaoUserResponse` 원본 | provider 전용 DTO를 `OAuthUserInfo`로 변환한 뒤 API body에는 `UserProfile`만 넣는다. |
| 로그인 성공 시 `error` 데이터 | 성공 응답에서는 `ApiResponse.data`에 값이 있고 `error`는 null이다. |
| Refresh Token 재발급 시 새 Refresh Token | `/auth/refresh`는 Access Token만 새로 만들며 Refresh Token rotation을 하지 않는다. 따라서 성공 응답에는 `TokenReissueResponse`만 있고 `Set-Cookie`도 설정하지 않는다. |
| 로그아웃 성공 body | `ResponseEntity<Void>`의 `204 No Content`라 JSON body가 없다. |
| 로그인 성공 시 state 삭제 쿠키 | `AuthController.oauthLogin`은 state 쿠키를 삭제하지 않는다. 서버에서는 이미 소비됐고 브라우저 쿠키는 TTL까지 남을 수 있다. |

## 9. 실패 및 “응답이 오지 않는” 경우

실제 HTTP 요청에서는 예외도 전역 처리기나 Security 설정을 통해 응답으로 변환된다. 다만 예외가 발생한 경우 Controller의 성공 응답과 Refresh Token `Set-Cookie`는 생성되지 않는다.

| 상황 | 발생 위치 | HTTP 응답 |
| --- | --- | --- |
| `provider`, `authorizationCode`, `state`가 null/blank | `@Valid OAuthLoginRequest` | `400 BAD_REQUEST`, `data: null`, `error.details`에 필드별 오류 |
| JSON 형식 자체가 잘못됨 | `GlobalExceptionHandler.handleUnreadableMessage` | `400 BAD_REQUEST` |
| state 쿠키 없음, body state 불일치, 만료·재사용 | `OAuthStateService.consume` | `401 UNAUTHORIZED`, 인가 코드 관련 인증 실패 메시지 |
| 지원하지 않는 provider | `AuthService.findProviderClient` | `400 BAD_REQUEST`, `details[0].field = provider` |
| Kakao token/user API가 4xx/5xx 응답 | `KakaoOAuthProviderClient` | `401 UNAUTHORIZED`, provider 인증 실패 메시지 |
| Kakao access token 또는 user id 누락 | Kakao client 내부 `IllegalStateException` | `/auth/oauth` 기준 `500 INTERNAL_SERVER_ERROR` |
| provider 사용자 정보의 ID/nickname 누락 | `AuthService.validateUserInfo` | `401 UNAUTHORIZED` |
| 연결된 내부 사용자가 비활성·탈퇴 상태 | `AccountProvisioningService`/`AuthService.ensureActive` | `401 UNAUTHORIZED` |
| 소셜 계정 동시 연결 충돌 | `AccountProvisioningService` | `409 SOCIAL_ACCOUNT_CONFLICT` |
| 위에서 예상하지 못한 서버 예외 | `GlobalExceptionHandler.handleUnexpectedException` | `500 INTERNAL_SERVER_ERROR`, 내부 원인은 body에 노출하지 않음 |

오류 body의 예시는 다음과 같다.

```json
{
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증에 실패했습니다. 인가 코드가 만료되었거나 유효하지 않습니다.",
    "details": [
      {
        "field": "authorizationCode",
        "reason": "만료되었거나 이미 사용된 인가 코드입니다."
      }
    ]
  }
}
```

로그인 요청이 실패하면 성공 시에만 설정되는 `refresh_token` 쿠키는 내려오지 않는다. state 검증을 통과한 뒤 실패한 경우에는 기존 `oauth_state` 쿠키 삭제 header도 내려오지 않으며, 이미 서버에서 해당 state가 소비되었을 수 있다.

## 10. 로그인 이후 흐름

### 10.1 Access Token 재발급

#### 요청

```http
POST /auth/refresh
Cookie: refresh_token={원본 Refresh Token}
```

`/auth/refresh`는 공개 URL이지만, 쿠키가 없거나 유효하지 않으면 `RefreshTokenService.requireValidUser`가 `401`을 반환한다.

#### 호출 순서

```text
AuthController.reissueToken(refreshToken)
  -> AuthService.reissueToken(refreshToken)
      -> RefreshTokenService.requireValidUser(refreshToken)
          -> SHA-256(refreshToken)
          -> RefreshTokenRepository.findByTokenHashAndDeletedAtIsNull
      -> ensureActive(user)
      -> AccessTokenIssuer.issue(user)
      -> TokenReissueResponse
```

성공 body는 다음과 같다. 상태 코드는 `200 OK`이며 Refresh Token 쿠키는 새로 내려오지 않는다.

```json
{
  "data": {
    "accessToken": "{새 애플리케이션 JWT}",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "error": null
}
```

### 10.2 로그아웃

#### 실제 요청 조건

```http
POST /auth/logout
Authorization: Bearer {유효한 Access Token}
Cookie: refresh_token={원본 Refresh Token}
```

`SecurityConfig`에서 `/auth/logout`은 `permitAll` 목록에 없기 때문에 Access Token 검증이 먼저 수행된다.

#### 호출 순서

```text
JwtAuthenticationFilter
  -> JwtDecoder.decode(accessToken)
  -> SecurityContext에 principal/role 저장
  -> AuthController.logout(refreshToken)
      -> AuthService.logout(refreshToken)
          -> RefreshTokenService.revoke(refreshToken)
      -> deleteRefreshTokenCookie()
      -> 204 No Content
```

Refresh Token 쿠키가 없으면 `RefreshTokenService.revoke`는 아무 작업도 하지 않지만, 유효한 Access Token으로 Controller까지 도달한 경우에는 쿠키 삭제 응답과 `204`를 반환한다.

Access Token이 없거나 잘못됐거나 만료됐다면 Controller가 호출되지 않는다.

```json
{
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "로그인이 필요하거나 Access Token이 만료되었거나 유효하지 않습니다.",
    "details": []
  }
}
```

이 경우 Refresh Token 쿠키 삭제 `Set-Cookie`도 내려오지 않는다.

### 10.3 회원 탈퇴

```text
DELETE /users
Authorization: Bearer {유효한 Access Token}
  -> JwtAuthenticationFilter가 principal=userId 구성
  -> UserController.withdraw(authentication)
  -> AccountWithdrawalService.withdraw(userId)
      -> User.withdraw(now)
      -> RefreshTokenRepository.revokeAllByUserId(userId, now)
  -> 204 No Content
```

## 11. 구현 파일 빠른 링크

- API 진입: [`AuthController.java`](../src/main/java/com/example/KTB_Agile_backend/auth/controller/AuthController.java)
- 인증 조합: [`AuthService.java`](../src/main/java/com/example/KTB_Agile_backend/auth/service/AuthService.java)
- state: [`OAuthStateService.java`](../src/main/java/com/example/KTB_Agile_backend/auth/service/OAuthStateService.java)
- Kakao 통신: [`KakaoOAuthProviderClient.java`](../src/main/java/com/example/KTB_Agile_backend/auth/client/KakaoOAuthProviderClient.java)
- 계정 생성·조회: [`AccountProvisioningService.java`](../src/main/java/com/example/KTB_Agile_backend/user/service/AccountProvisioningService.java)
- Access Token: [`JwtAccessTokenIssuer.java`](../src/main/java/com/example/KTB_Agile_backend/auth/token/JwtAccessTokenIssuer.java)
- Refresh Token: [`RefreshTokenService.java`](../src/main/java/com/example/KTB_Agile_backend/auth/service/RefreshTokenService.java)
- 보안 접근 제어: [`SecurityConfig.java`](../src/main/java/com/example/KTB_Agile_backend/security/SecurityConfig.java)
- 공통 오류 응답: [`GlobalExceptionHandler.java`](../src/main/java/com/example/KTB_Agile_backend/common/exception/GlobalExceptionHandler.java)
