# 인증 DTO 역할과 사용 시나리오

## 현재 인증 범위

- 실제 OAuth provider는 Kakao만 사용한다.
- 로그인 성공 시 Access Token은 응답 본문으로 반환한다.
- Refresh Token은 응답 본문에 넣지 않고 HttpOnly 쿠키로 전달한다.
- Access Token 재발급은 Refresh Token 쿠키를 사용한다. 로그아웃은 유효한 Access Token으로 endpoint 인증을 통과한 뒤 Refresh Token 쿠키를 사용한다.
- 첫 Kakao 로그인에서는 서버가 User와 SocialAccount을 생성한다.
- JWT의 role은 서버가 발급하며, 클라이언트 요청 DTO로 받지 않는다.

## API 명세서 URI 표기와 Controller 매핑

API 명세서의 URI 앞에 붙은 `/api/`는 실제 서버 경로가 아니라 API 명세서에 정의된 URI임을 나타내는 표기다. Controller를 구현할 때는 `/api/`를 제거한 실제 경로를 사용한다.

예를 들어 API 명세서의 `/api/auth/oauth`는 실제 `/auth/oauth`로 매핑한다.

```java
@RequestMapping("/auth")
class AuthController {

	@PostMapping("/oauth")
	void oauthLogin() {
	}
}
```

따라서 `/api/auth/refresh`는 실제 `/auth/refresh`, `/api/auth/logout`은 실제 `/auth/logout`이다. 메서드에는 공통 경로를 제외한 `/refresh`, `/logout`만 작성한다.

## DTO 목록

| DTO | 역할 | 사용 시점 |
| --- | --- | --- |
| `OAuthLoginRequest` | OAuth callback에 필요한 provider, authorization code, state를 전달한다. | Kakao 인증 코드로 로그인을 요청할 때 |
| `OAuthStateResponse` | 일회성 OAuth state와 만료 시간을 반환한다. | 로그인 시작 전에 state를 발급할 때 |
| `OAuthUserInfo` | provider 응답을 서비스가 사용하는 공통 사용자 정보로 변환한 내부 DTO다. | Kakao 사용자 조회가 끝난 뒤 |
| `KakaoTokenResponse` | Kakao token API 응답에서 access token을 받는다. | Kakao authorization code를 token으로 교환할 때 |
| `KakaoUserResponse` | Kakao 사용자 API 응답을 타입으로 받는다. | Kakao 사용자 정보를 조회할 때 |
| `AuthResponse` | Access Token, 신규 사용자 여부, 사용자 요약 정보를 반환한다. | OAuth 로그인 성공 시 |
| `UserProfile` | 로그인 응답에 포함되는 사용자 요약 정보다. | `AuthResponse` 내부 |
| `TokenReissueResponse` | 새 Access Token, 만료 시간, 거래 취향 설정 필요 여부를 반환한다. | Refresh Token으로 재발급할 때 |
| `ApiResponse<T>` | 성공 데이터와 오류 영역을 동일한 응답 형식으로 감싼다. | Controller의 공통 응답 형식 |
| `ErrorResponse` | 오류 코드, 메시지, validation field 오류를 전달한다. | 4xx/5xx 오류 응답 |

## 시나리오별 흐름

### 1. OAuth 로그인 시작

1. 서버가 `OAuthStateService`로 state를 생성한다.
2. 서버는 원본 state를 `OAuthStateResponse`로 반환한다.
3. 서버는 같은 값을 HttpOnly 쿠키에 저장한다.

### 2. Kakao OAuth 로그인

1. Kakao가 백엔드 `GET /auth/kakao/callback`으로 authorization code와 state를 redirect한다. 또는 클라이언트가 `POST /auth/oauth`로 `OAuthLoginRequest`를 직접 보낼 수 있다.
2. callback Controller는 query parameter를 `OAuthLoginRequest`로 구성하고, 두 방식 모두 같은 로그인 서비스를 호출한다.
3. POST 방식의 body 필드는 `@NotBlank` validation으로 검증하고, callback 방식의 `code`와 `state`는 필수 query parameter로 받는다.
4. 서버가 쿠키 state와 요청 state를 비교하고 일회성 소비한다.
5. `KakaoTokenResponse`로 Kakao access token을 받고, `KakaoUserResponse`로 사용자 정보를 받는다.
6. 서버는 이를 `OAuthUserInfo`로 변환한다.
7. 최초 로그인이라면 User와 SocialAccount을 생성한다.
8. callback 방식은 Refresh Token을 쿠키에 저장한 뒤 `FRONTEND_REDIRECT_URI`로 `302 Found` redirect한다. 프론트엔드는 redirect 후 `/auth/refresh`를 호출해 `AuthResponse`가 아닌 `TokenReissueResponse`의 Access Token을 받는다. POST 방식은 `AuthResponse`를 JSON으로 반환한다.

### 3. Access Token 재발급

1. 클라이언트는 Refresh Token 쿠키를 포함해 재발급을 요청한다.
2. 서버가 쿠키의 Refresh Token을 검증한다.
3. 성공하면 `TokenReissueResponse`로 새 Access Token을 반환한다.

Refresh Token이 쿠키에 있으므로 `TokenReissueRequest`는 만들지 않는다.

### 4. 로그아웃

1. `/auth/logout`은 공개 경로가 아니므로 서버가 Access Token을 먼저 인증한다.
2. 인증을 통과하면 서버가 Refresh Token 쿠키를 조회한다.
3. 저장된 Refresh Token을 폐기한다.
4. Refresh Token 쿠키를 삭제하고 `204 No Content`를 반환한다.

Access Token이 없거나 유효하지 않으면 Controller가 호출되지 않고 `401 UNAUTHORIZED`가 반환되며, Refresh Token 쿠키도 삭제되지 않는다.

본문이 없으므로 `LogoutRequest`와 `LogoutResponse`는 만들지 않는다.

## 현재 추가하지 않는 DTO

- `RefreshTokenResponse`: Refresh Token을 JSON으로 노출하지 않는다.
- `TokenReissueRequest`: Refresh Token을 쿠키에서 읽는다.
- `LogoutRequest`, `LogoutResponse`: 쿠키 폐기만 수행하고 204를 반환한다.
- `AccountProvisioningRequest`: 현재는 첫 OAuth 로그인 과정에서 User를 자동 생성한다.
- `UserProfileUpdateRequest`: 프로필 수정 API가 확정될 때 추가한다.
- 이메일 회원가입 DTO: 현재 프로젝트 범위에 포함하지 않는다.
