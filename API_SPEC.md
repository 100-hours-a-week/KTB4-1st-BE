# KTB-Agile-backend API 명세서

현재 소스 코드의 컨트롤러, 요청/응답 DTO, 보안 설정을 기준으로 작성한 로컬 테스트용 명세서입니다.

## 1. 기본 정보

| 항목 | 값 |
|---|---|
| Postman API Base URL | http://127.0.0.1:8080 |
| Kakao Redirect URI | http://localhost:8080/auth/kakao/callback |
| Callback 이후 프론트엔드 URI | http://localhost:3000 |
| Content-Type | application/json |
| Access Token 만료 | 900초 |
| Refresh Token 만료 | 14일 |
| OAuth state 만료 | 300초 |

Postman 요청은 현재 환경에서 정상 확인된 127.0.0.1을 사용합니다.
카카오 콘솔의 Redirect URI와 Spring 환경변수 KAKAO_REDIRECT_URI는 카카오에 등록한
http://localhost:8080/auth/kakao/callback과 정확히 일치해야 합니다.

localhost와 127.0.0.1은 쿠키 저장 위치와 로컬 네트워크 처리에서 다르게 동작할 수 있습니다.
따라서 Postman API 요청에서는 127.0.0.1을 일관되게 사용하고, state 쿠키도 같은 호스트에 저장합니다.

## 2. 공통 응답 형식

성공 응답은 다음 형식입니다.

~~~json
{
  "data": {},
  "error": null
}
~~~

실패 응답은 다음 형식입니다.

~~~json
{
  "data": null,
  "error": {
    "code": "BAD_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "details": [
      {
        "field": "fieldName",
        "reason": "상세 사유"
      }
    ]
  }
}
~~~

주요 HTTP 상태 코드는 다음과 같습니다.

| 상태 | 의미 |
|---:|---|
| 200 | 정상 처리 |
| 201 | 신규 회원 생성과 함께 로그인 성공 |
| 204 | 정상 처리, 응답 본문 없음 |
| 400 | 요청 본문/필수 값이 잘못됨 |
| 401 | access token, refresh token, state 또는 Kakao 인가 코드 인증 실패 |
| 403 | 권한 부족 |
| 409 | 소셜 계정 충돌 |
| 500 | 서버 내부 오류 |

애플리케이션이 처리한 오류는 JSON 응답입니다. Tomcat의 HTML 400 Bad Request가 보이면
요청 URL, HTTP 메서드, JSON 문법, Postman Agent 또는 로컬 호스트를 먼저 확인합니다.

## 3. 인증 및 쿠키

### Access Token

보호된 API를 호출할 때 다음 헤더를 사용합니다.

~~~http
Authorization: Bearer <accessToken>
~~~

현재 보호된 API는 POST /auth/logout과 DELETE /users입니다.

### refresh_token 쿠키

OAuth 로그인 성공 시 서버가 refresh_token HttpOnly 쿠키를 내려줍니다.
Refresh와 Logout 요청에는 이 쿠키가 필요합니다.

~~~http
Cookie: refresh_token=<refreshToken>
~~~

Postman 쿠키 관리자가 자동으로 쿠키를 붙이지 않으면 Cookies 메뉴에서
127.0.0.1 도메인에 쿠키를 추가하거나 위 Cookie 헤더를 직접 넣습니다.

### oauth_state 쿠키

state 발급 응답에서 서버가 oauth_state HttpOnly 쿠키를 내려줍니다.
로그인 요청에서는 다음 두 값이 모두 필요합니다.

1. JSON body의 state
2. Cookie의 oauth_state

두 값은 완전히 같아야 합니다. state는 한 번 사용되거나 300초가 지나면 유효하지 않습니다.
state는 Authorization 헤더에 넣는 값이 아닙니다.

## 4. API 목록

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | /auth/oauth/state | 없음 | OAuth state 발급 |
| POST | /auth/oauth | state 쿠키 필요 | Kakao 인가 코드로 로그인 |
| POST | /auth/refresh | refresh 쿠키 필요 | access token 재발급 |
| POST | /auth/logout | Bearer + refresh 쿠키 | 로그아웃 및 refresh token 폐기 |
| DELETE | /users | Bearer | 회원 탈퇴 처리 |

---

## 5. OAuth state 발급

### GET /auth/oauth/state

카카오 로그인 시작 전에 일회용 state를 발급합니다.

#### Request

~~~http
GET {{baseUrl}}/auth/oauth/state
~~~

요청 본문과 인증 헤더는 필요하지 않습니다.

#### Response: 200 OK

~~~json
{
  "data": {
    "state": "<state>",
    "expiresIn": 300
  },
  "error": null
}
~~~

응답 헤더에는 다음과 유사한 쿠키가 포함됩니다.

~~~http
Set-Cookie: oauth_state=<state>; Path=/auth; Max-Age=300; HttpOnly; Secure; SameSite=Lax
~~~

응답 body의 data.state와 쿠키의 oauth_state 값이 같아야 합니다.

---

## 6. Kakao OAuth 로그인

### GET /auth/kakao/callback

Kakao 콘솔의 Redirect URI는 백엔드 callback 주소로 설정합니다. Kakao 인증이 끝나면 Kakao가 아래 endpoint로 브라우저를 redirect하고 `code`, `state`를 query parameter로 전달합니다.

```text
http://localhost:8080/auth/kakao/callback
```

`KAKAO_REDIRECT_URI` 환경변수와 Kakao 콘솔에 등록한 Redirect URI는 완전히 같아야 합니다.

#### Request

~~~http
GET {{redirect_uri}}?code=<authorization-code>&state=<state>
Cookie: oauth_state=<state>
~~~

callback은 state 검증 후 Kakao token API와 사용자 API를 호출하고, 내부 계정 및 애플리케이션 토큰을 처리합니다. 성공하면 `302 Found`와 `Location: ${FRONTEND_REDIRECT_URI}`를 반환하고, Refresh Token은 `Set-Cookie`로 전달합니다. Access Token은 URL이나 callback 응답 body에 넣지 않습니다.

프론트엔드는 redirect된 화면에서 `POST /auth/refresh`를 호출해 Refresh Token 쿠키 기반으로 Access Token을 받아야 합니다.

---

### POST /auth/oauth

Kakao에서 받은 일회용 authorization code를 Spring 서버에 전달하여 로그인합니다.

#### Request headers

~~~http
Content-Type: application/json
Cookie: oauth_state=<state>
~~~

Authorization 헤더는 사용하지 않습니다.

#### Request body

Postman에서 Body → raw → JSON을 선택하고 다음을 입력합니다.

~~~json
{
  "provider": "KAKAO",
  "authorizationCode": "<fresh-authorization-code>",
  "state": "<same-state>"
}
~~~

검증 규칙:

- provider는 KAKAO여야 합니다.
- authorizationCode는 비어 있지 않아야 합니다.
- state는 body와 oauth_state 쿠키가 같아야 합니다.
- authorization code는 짧은 시간 동안 한 번만 사용할 수 있습니다.

#### Request example

~~~http
POST {{baseUrl}}/auth/oauth
Content-Type: application/json
Cookie: oauth_state=<state>

{
  "provider": "KAKAO",
  "authorizationCode": "<fresh-authorization-code>",
  "state": "<state>"
}
~~~

#### Response: 201 Created - 신규 회원

~~~json
{
  "data": {
    "accessToken": "<accessToken>",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "isNewUser": true,
    "user": {
      "userId": 1,
      "nickname": "<nickname>",
      "profileImageUrl": "<profileImageUrl>"
    }
  },
  "error": null
}
~~~

#### Response: 200 OK - 기존 회원

응답 구조는 동일하며 isNewUser가 false입니다.

로그인 성공 시 응답 헤더에 refresh token 쿠키가 추가됩니다.

~~~http
Set-Cookie: refresh_token=<refreshToken>; HttpOnly; Max-Age=1209600
~~~

#### 주요 실패 응답

| 상태 | 상황 |
|---:|---|
| 400 | body 누락, JSON 문법 오류, 필수 값 누락 |
| 401 | state 쿠키 누락/불일치, 만료·사용된 인가 코드, Kakao 인증 실패 |
| 409 | 소셜 계정 충돌 |
| 500 | 예상하지 못한 서버 또는 외부 API 오류 |

인가 코드가 잘못된 경우의 예시는 다음과 같습니다.

~~~json
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
~~~

---

## 7. Access token 재발급

### POST /auth/refresh

refresh token 쿠키로 새 access token을 발급합니다.

#### Request

~~~http
POST {{baseUrl}}/auth/refresh
Cookie: refresh_token=<refreshToken>
~~~

요청 body는 없습니다. Authorization 헤더도 필요하지 않습니다.

#### Response: 200 OK

~~~json
{
  "data": {
    "accessToken": "<new-accessToken>",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "needsPreferenceSetup": true
  },
  "error": null
}
~~~

#### 주요 실패 응답

| 상태 | 상황 |
|---:|---|
| 401 | refresh token 쿠키 누락, 만료, 폐기 또는 위조 |
| 500 | 예상하지 못한 서버 오류 |

---

## 8. 로그아웃

### POST /auth/logout

현재 access token을 확인하고 refresh token을 폐기합니다.

#### Request

~~~http
POST {{baseUrl}}/auth/logout
Authorization: Bearer <accessToken>
Cookie: refresh_token=<refreshToken>
~~~

요청 body는 없습니다.

#### Response: 204 No Content

정상 처리 시 응답 본문이 없습니다. 서버는 refresh token 쿠키를 삭제합니다.

~~~http
Set-Cookie: refresh_token=; Max-Age=0
~~~

로그아웃 후 같은 refresh token으로 /auth/refresh를 호출하면 401 Unauthorized가
반환되어야 합니다.

---

## 9. 회원 탈퇴

### DELETE /users

현재 로그인한 사용자의 탈퇴 처리를 수행합니다.

#### Request

~~~http
DELETE {{baseUrl}}/users
Authorization: Bearer <accessToken>
~~~

요청 body는 없습니다.

#### Response: 204 No Content

현재 구현은 탈퇴 처리용 API이며, 테스트 계정에 실제 상태 변경이 발생합니다.
다른 API 테스트를 모두 끝낸 뒤 마지막에 실행합니다.

#### 주요 실패 응답

| 상태 | 상황 |
|---:|---|
| 401 | access token 누락, 만료 또는 위조 |
| 500 | 예상하지 못한 서버 오류 |

---

## 10. 프론트엔드 없이 Kakao 로그인 테스트

현재 코드에는 `/auth/kakao/callback` 컨트롤러가 있습니다. Kakao Redirect URI를 이
백엔드 주소로 설정하면 callback부터 로그인 처리까지 백엔드가 수행합니다.

### 순서

1. Spring을 실행합니다.
2. GET /auth/oauth/state를 호출합니다.
3. 응답의 data.state를 복사하고, 같은 값이 oauth_state 쿠키에 있는지 확인합니다.
4. 아래 Kakao authorize URL을 실제 값으로 채워 브라우저에서 엽니다.

~~~text
https://kauth.kakao.com/oauth/authorize?client_id=<KAKAO_REST_API_KEY>&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fauth%2Fkakao%2Fcallback&response_type=code&state=<state>
~~~

5. Kakao가 `http://localhost:8080/auth/kakao/callback?code=...&state=...`로
   redirect하면 백엔드가 callback을 처리합니다.
6. callback 응답의 `Location`으로 프론트엔드에 이동합니다. 응답 쿠키의
   refresh_token을 유지한 채 `POST /auth/refresh`를 호출해 받은 accessToken을
   보호된 API 테스트에 사용합니다.

인가 코드는 재사용할 수 없습니다. 같은 code를 다시 보내면 실패하므로,
실패 후에는 반드시 state와 code를 모두 새로 발급받습니다.

## 11. Postman 환경변수

Postman Environment 이름은 자유롭게 정해도 되지만, 요청에 사용하는 변수명은 아래와
같아야 합니다.

| 변수명 | 값 | 용도 |
|---|---|---|
| baseUrl | http://127.0.0.1:8080 | Postman API 주소 |
| kakao_client_id | Kakao REST API 키 | authorize URL 작성 |
| redirect_uri | http://localhost:8080/auth/kakao/callback | Kakao redirect URI 확인 |
| oauth_state | 처음에는 빈 값 | state 발급 후 저장 |
| authorization_code | 처음에는 빈 값 | Kakao callback의 code 저장 |
| access_token | 처음에는 빈 값 | 로그인/refresh 응답의 access token 저장 |
| refresh_token | 처음에는 빈 값 | 필요할 때만 수동 저장 |

JWT secret, Kakao client secret, Kakao Admin key는 Postman 요청에 넣지 않습니다.
이 값들은 Spring 실행 환경변수로만 관리합니다.

## 12. 권장 테스트 순서

| 순서 | 요청 | 정상 기대값 |
|---:|---|---|
| 1 | GET /auth/oauth/state | 200, state body와 쿠키 발급 |
| 2 | Kakao authorize URL | callback URL에 code와 state 포함 |
| 3 | GET /auth/kakao/callback | 302, `FRONTEND_REDIRECT_URI`로 이동하고 refresh 쿠키 발급 |
| 4 | POST /auth/refresh | 200, 새 access token |
| 5 | POST /auth/logout | 204, refresh 쿠키 삭제 |
| 6 | POST /auth/refresh 재호출 | 401, 폐기된 refresh token |
| 7 | DELETE /users | 테스트 계정인 경우에만 마지막으로 실행 |

### 의도적인 실패 테스트

다음 테스트는 정상 흐름 확인 후 실행합니다.

1. POST /auth/oauth body를 {}로 보내기 → 400
2. OAuth 로그인에서 oauth_state 쿠키 제거 → 401
3. 이미 성공한 authorization code 재사용 → 401
4. POST /auth/refresh에서 refresh cookie 제거 → 401
5. 보호된 API에서 Authorization 헤더 제거 → 401

## 13. 현재 구현의 범위와 제한

- 현재 API 목록에는 사용자 조회와 token introspection API가 없습니다.
- Kakao callback은 백엔드 `/auth/kakao/callback` 컨트롤러가 처리합니다.
- callback 성공 후 `FRONTEND_REDIRECT_URI`로 `302 Found` redirect하고 Refresh Token 쿠키를 반환합니다. 프론트엔드는 `/auth/refresh`로 Access Token을 받습니다.
- 메인 application.yaml에는 외부 DB 접속 설정이 없습니다.
- 현재 로컬 실행에서는 임베디드 H2 인메모리 DB가 사용되며, 애플리케이션을 재시작하면
  사용자, refresh token, OAuth state 데이터가 초기화될 수 있습니다.
- Spring 실행에는 다음 환경변수가 필요합니다.

| 환경변수 | 필수 여부 | 기본값/조건 |
|---|---|---|
| JWT_SECRET | 필수 | 32바이트 이상 |
| ACCESS_TOKEN_TTL_SECONDS | 선택 | 900 |
| REFRESH_TOKEN_TTL_DAYS | 선택 | 14 |
| OAUTH_STATE_TTL_SECONDS | 선택 | 300 |
| FRONTEND_REDIRECT_URI | 선택 | http://localhost:3000 |
| KAKAO_CLIENT_ID | 필수 | Kakao REST API 키 |
| KAKAO_CLIENT_SECRET | 설정 시 사용 | Kakao 클라이언트 시크릿 |
| KAKAO_REDIRECT_URI | 필수 | Kakao 콘솔 등록 값과 완전 일치 |

## 14. 표 형식 전체 API 명세

아래 표는 제공해주신 형식을 따르되, 현재 백엔드 코드와 실제 동작에 맞춘 버전입니다.

### 14-1. 제공 표와 현재 구현의 차이

| 항목 | 제공해주신 표 | 현재 구현 |
|---|---|---|
| 로그인 URL | /api/auth/oauth | /auth/oauth (직접 전달), /auth/kakao/callback (Kakao callback) |
| Kakao Redirect URI | 명세에 없음 | /auth/kakao/callback |
| 로그인 body | provider, authorizationCode | provider, authorizationCode, state |
| Access Token 만료 | 1800초 | 기본 900초 |
| Refresh Cookie Path | /api/auth | /auth |
| OAuth state | 명세에 없음 | body의 state와 oauth_state 쿠키 모두 필수 |

### 14-2. 성공 API 명세

| 기능 | Method | URL | Request Header | Request Body | 필수 / 검증 및 쿠키 | Response Status | Response Body |
|---|---|---|---|---|---|---|---|
| Kakao callback | GET | /auth/kakao/callback | Cookie: oauth_state=state 값 | 없음 | query `code`, `state` 필수<br>query state와 쿠키 state가 같아야 합니다.<br>응답 시 refresh_token 쿠키 발급 | 302 | `Location: FRONTEND_REDIRECT_URI`<br>응답 body 없음 |
| OAuth state 발급 | GET | /auth/oauth/state | Accept: application/json (선택) | 없음 | 응답 시 oauth_state 쿠키 발급<br>HttpOnly; Secure; SameSite=Lax; Path=/auth; Max-Age=300 | 200 | {<br>  "data": {<br>    "state": "state 값",<br>    "expiresIn": 300<br>  },<br>  "error": null<br>} |
| 회원가입 | POST | /auth/oauth | Content-Type: application/json<br>Accept: application/json (선택)<br>Cookie: oauth_state=state 값 | {<br>  "provider": "KAKAO",<br>  "authorizationCode": "카카오 인가 코드",<br>  "state": "state 값"<br>} | 필수<br>provider: String<br>authorizationCode: String<br>state: String<br><br>body.state와 oauth_state 쿠키가 같아야 합니다.<br>응답 시 refresh_token 쿠키 발급<br>HttpOnly; Secure; SameSite=Lax; Path=/auth; Max-Age=1209600 | 201 | {<br>  "data": {<br>    "accessToken": "우리 서비스 Access Token",<br>    "tokenType": "Bearer",<br>    "expiresIn": 900,<br>    "isNewUser": true,<br>    "user": {<br>      "userId": 101,<br>      "nickname": "닉네임",<br>      "profileImageUrl": "https://example.com/profile.png"<br>    }<br>  },<br>  "error": null<br>} |
| 로그인 | POST | /auth/oauth | Content-Type: application/json<br>Accept: application/json (선택)<br>Cookie: oauth_state=state 값 | {<br>  "provider": "KAKAO",<br>  "authorizationCode": "카카오 인가 코드",<br>  "state": "state 값"<br>} | 필수<br>provider: String<br>authorizationCode: String<br>state: String<br><br>기존 사용자라면 isNewUser는 false입니다.<br>응답 시 refresh_token 쿠키 발급<br>HttpOnly; Secure; SameSite=Lax; Path=/auth; Max-Age=1209600 | 200 | {<br>  "data": {<br>    "accessToken": "우리 서비스 Access Token",<br>    "tokenType": "Bearer",<br>    "expiresIn": 900,<br>    "isNewUser": false,<br>    "user": {<br>      "userId": 101,<br>      "nickname": "닉네임",<br>      "profileImageUrl": "https://example.com/profile.png"<br>    }<br>  },<br>  "error": null<br>} |
| Access Token 재발급 | POST | /auth/refresh | Accept: application/json (선택)<br>Cookie: refresh_token=refresh token 값 | 없음 | refresh_token 쿠키 필수<br>Authorization 헤더 불필요 | 200 | {<br>  "data": {<br>    "accessToken": "새 Access Token",<br>    "tokenType": "Bearer",<br>    "expiresIn": 900,<br>    "needsPreferenceSetup": true<br>  },<br>  "error": null<br>} |
| 로그아웃 | POST | /auth/logout | Accept: application/json (선택)<br>Authorization: Bearer Access Token<br>Cookie: refresh_token=refresh token 값 | 없음 | Access Token과 refresh_token 쿠키 필요<br>정상 처리 후 refresh_token 쿠키 삭제 | 204 | 응답 본문 없음 |
| 회원 탈퇴 | DELETE | /users | Accept: application/json (선택)<br>Authorization: Bearer Access Token | 없음 | Access Token 필요<br>테스트 계정의 상태가 변경되므로 모든 테스트 마지막에 실행 | 204 | 응답 본문 없음 |

### 14-3. 실패 API 명세

| 기능 | Response Status | Response Body |
|---|---:|---|
| POST /auth/oauth | 400 | {<br>  "data": null,<br>  "error": {<br>    "code": "BAD_REQUEST",<br>    "message": "인가 코드가 유효하지 않습니다.",<br>    "details": [<br>      {<br>        "field": "authorizationCode",<br>        "reason": "유효하지 않은 인가 코드입니다."<br>      },<br>      {<br>        "field": "state",<br>        "reason": "state는 필수 입력값입니다."<br>      }<br>    ]<br>  }<br>} |
| POST /auth/oauth | 401 | {<br>  "data": null,<br>  "error": {<br>    "code": "UNAUTHORIZED",<br>    "message": "인증에 실패했습니다. 인가 코드가 만료되었거나 유효하지 않습니다.",<br>    "details": [<br>      {<br>        "field": "authorizationCode",<br>        "reason": "만료되었거나 이미 사용된 인가 코드입니다."<br>      }<br>    ]<br>  }<br>} |
| POST /auth/oauth | 409 | {<br>  "data": null,<br>  "error": {<br>    "code": "SOCIAL_ACCOUNT_CONFLICT",<br>    "message": "계정 연결 정보가 충돌했습니다.",<br>    "details": [<br>      {<br>        "field": "socialAccount",<br>        "reason": "해당 계정이 다른 사용자와 연결되어 있습니다."<br>      }<br>    ]<br>  }<br>} |
| POST /auth/oauth | 500 | {<br>  "data": null,<br>  "error": {<br>    "code": "INTERNAL_SERVER_ERROR",<br>    "message": "인증 처리 중 서버 오류가 발생했습니다.",<br>    "details": []<br>  }<br>} |
| POST /auth/refresh | 401 | {<br>  "data": null,<br>  "error": {<br>    "code": "UNAUTHORIZED",<br>    "message": "Refresh Token이 만료되었거나 유효하지 않습니다.",<br>    "details": []<br>  }<br>} |
| POST /auth/refresh | 500 | {<br>  "data": null,<br>  "error": {<br>    "code": "INTERNAL_SERVER_ERROR",<br>    "message": "토큰 재발급 중 서버 오류가 발생했습니다.",<br>    "details": []<br>  }<br>} |
| POST /auth/logout | 401 | {<br>  "data": null,<br>  "error": {<br>    "code": "UNAUTHORIZED",<br>    "message": "로그인이 필요하거나 Access Token이 만료되었거나 유효하지 않습니다.",<br>    "details": []<br>  }<br>} |
| POST /auth/logout | 500 | {<br>  "data": null,<br>  "error": {<br>    "code": "INTERNAL_SERVER_ERROR",<br>    "message": "로그아웃 처리 중 서버 오류가 발생했습니다.",<br>    "details": []<br>  }<br>} |
| DELETE /users | 401 | {<br>  "data": null,<br>  "error": {<br>    "code": "UNAUTHORIZED",<br>    "message": "로그인이 필요합니다.",<br>    "details": []<br>  }<br>} |

### 14-4. 프론트엔드 요청 예시

#### Kakao callback 로그인 요청

Kakao 콘솔의 Redirect URI를 백엔드 callback 주소로 설정하면 Kakao가 아래 요청을 브라우저에서 자동으로 호출합니다.

~~~http
GET /auth/kakao/callback?code=카카오 인가 코드&state=state 값
Cookie: oauth_state=state 값
~~~

callback 성공 후 `Location: FRONTEND_REDIRECT_URI`로 redirect하고 Refresh Token 쿠키를 반환합니다. 프론트엔드는 이동 후 아래처럼 `/auth/refresh`를 호출해 Access Token을 받습니다.

~~~http
POST /auth/refresh
Cookie: refresh_token=refresh token 값
~~~

#### 직접 로그인 요청(대체 방식)

~~~http
POST /auth/oauth
Content-Type: application/json
Cookie: oauth_state=state 값

{
  "provider": "KAKAO",
  "authorizationCode": "카카오 인가 코드",
  "state": "state 값"
}
~~~

#### 보호된 API 요청

~~~http
DELETE /users
Authorization: Bearer Access Token
~~~

프론트엔드가 다른 포트에서 실행되면 로그인, refresh, logout 요청에 쿠키를 포함해야
하므로 fetch의 credentials 설정을 include로 사용해야 합니다.

제공해주신 표처럼 URL을 /api/auth/oauth로 확정하려면 컨트롤러뿐 아니라 보안 설정,
예외 메시지 분기, 프론트 요청 URL도 함께 변경해야 합니다. 현재 코드는 /auth 경로가
실제 계약입니다.
