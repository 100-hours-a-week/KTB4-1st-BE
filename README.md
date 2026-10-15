# KTB Final Team Backend

Spring Boot backend project built with Java 25 and Gradle.

## Run

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-server-host:3306/ktb \
SPRING_DATASOURCE_USERNAME=ktb \
SPRING_DATASOURCE_PASSWORD=your-password \
./gradlew bootRun
```

The `dev` profile is selected by default and connects to the MySQL server managed by Docker Compose outside this repository. Set the JDBC URL and credentials provided by that server's operator; Flyway applies pending migrations at startup. The dev profile uses a local-only JWT key when `JWT_SECRET` is unset. Tests continue to use an in-memory H2 database.
To select the production profile, set `SPRING_PROFILES_ACTIVE=prod`.

The database must be empty on its first Flyway startup; migrations create the schema.

The `prod` profile requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `AWS_S3_BUCKET`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_REDIRECT_URI`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, and `KAKAO_REDIRECT_URI`. Its schema mode defaults to `validate`.

## Test

```bash
./gradlew test
```

## S3 image flow

Set `AWS_S3_BUCKET`, `AWS_REGION`, and `AI_BASE_URL` (for example, `http://fastapi:8000`). Backend AI paths are fixed in code: `/api/vision/analyze-item`, `/api/moderation/check-text`, `/value/estimate-price`, `/api/search/rank-candidates`, and `/api/chatbot/ask`. Image analysis sends `POST {"imageUrls":["<url-1>","<url-2>"]}` for 1–3 images. Its JSON response and HTTP status are forwarded to the frontend, including analysis errors. Connection and response timeouts can be set with `AI_CONNECT_TIMEOUT_SECONDS` and `AI_READ_TIMEOUT_SECONDS`. The bucket should remain private; item responses use 1-hour presigned image URLs by default (`AWS_S3_READ_URL_DURATION_SECONDS`), while AI receives a 10-minute URL.

When the user starts analysis, request 1–3 upload URLs with `POST /images/presigned-urls` and `{"images":[{"contentType":"image/jpeg"},{"contentType":"image/png"}]}`. Upload each image to S3 using its `uploadUrl` and `requiredHeaders`, then call `POST /images/ai-analysis` with `{"objectKeys":["<object-key-1>","<object-key-2>"]}`. For an abandoned upload, call `DELETE /images?objectKey=...`. New `POST /items` requests send `objectKeys`; `PUT /items/{itemId}` continues to use `imageIds`.

The first `objectKeys` entry when creating an item, or first `imageIds` entry when updating it, is the thumbnail image. The item detail response includes its `thumbnailImageId`.

The client must send the `requiredHeaders` returned by `POST /images/presigned-urls` with its S3 PUT, including when uploading one image. Configure bucket CORS to allow the frontend origin and the `PUT` method with the `Content-Type` and `x-amz-tagging` headers.

## Smart search

The backend calls the AI server's fixed `POST /api/search/rank-candidates` path under `AI_BASE_URL`. Call the backend's `POST /search/rank-candidates` with the selected `groupId`, `items` (`itemId` and `quantity`), and optional `preferenceText`, for example `{"groupId":1,"items":[{"itemId":101,"quantity":2}],"preferenceText":"야식으로 먹을 거 찾아요"}`. The backend sums each selected item's unit price multiplied by its requested quantity, fetches up to 30 recent requestable items in that group priced at no more than 120% of the total, and returns their summaries in the AI-ranked order in `data.items`. Missing or blank `preferenceText` returns candidates in recent-first order without calling AI; no candidates returns an empty list. AI failure messages are returned in the common API envelope's `error.message`.

## Chatbot

The backend calls the AI server's fixed `POST /api/chatbot/ask` path under `AI_BASE_URL`. Call the authenticated backend's `POST /chatbot/ask` with a nonblank `message` and up to 10 previous `history` messages, each with a `role` of `user` or `assistant` and nonblank `content`. Missing or null `history` is treated as empty. The backend forwards the supplied history to the AI server and does not load or save chat history; the frontend owns it for the modal session and clears it when the modal closes. A successful response is wrapped in `data` with `answer` and nullable `action`; `action.screen` is one of `MY_PAGE`, `ITEM_LIST`, `ITEM_ADD`, `CHAT_LIST`, `GROUP_MANAGE`, `INTEREST_LIST`, or `INQUIRY`. The frontend maps that identifier to its route and displays `action.label` as the button text. AI errors are returned in the common API envelope as HTTP 500; an AI validation response (HTTP 422) is also mapped to the backend's chatbot failure response.

## Item text moderation

The backend calls the AI server's fixed `POST /api/moderation/check-text` path under `AI_BASE_URL`. Before creating an item, call `POST /moderation-checks` with `title` and `content`. The response includes `isAppropriate`, `rejectionReason`, and a five-minute `checkId` when approved. Send that `moderationCheckId` with the item payload to `POST /items`; it is bound to the authenticated user and checked title/content, and can be used once. Inappropriate content returns the AI rejection reason and no `checkId`.

Configure an S3 Lifecycle rule on the bucket with the tag filter `pending=true` and expiration after 1 day. This repository has no bucket IaC, so apply a rule like this to the S3 bucket separately:

```json
{
  "Rules": [
    {
      "ID": "expire-pending-images",
      "Status": "Enabled",
      "Filter": {
        "And": {
          "Prefix": "images/",
          "Tags": [{ "Key": "pending", "Value": "true" }]
        }
      },
      "Expiration": { "Days": 1 }
    }
  ]
}
```

The backend role needs `s3:PutObject`, `s3:GetObject`, `s3:GetObjectTagging`, `s3:PutObjectTagging`, and `s3:DeleteObject` on the image prefix.

Existing URL-backed image rows continue to work; newly uploaded images store their S3 key and receive fresh read URLs from item APIs.

## Database schema

In the `dev` and `prod` profiles, Flyway applies versioned migrations before Hibernate validates the schema. Point the server at an empty MySQL schema for its first startup; the database user needs permission to create tables, indexes, and foreign keys. Hibernate uses `ddl-auto=validate` and will not change the schema itself. After V1 has been deployed, add future schema changes as new versioned migrations instead of editing V1.

## WebSocket chat messages

Connect to `/ws` with STOMP `CONNECT` header `Authorization: Bearer <jwt>`. Subscribe to `/topic/chat/rooms/{chatRoomId}` and send `{"content":"안녕하세요"}` to `/app/chat/rooms/{chatRoomId}/messages`. The server persists the message before broadcasting it to room subscribers.

Fetch the latest chat messages with `GET /chat/rooms/{chatRoomId}/messages`. Pass the returned `nextCursor` as the `cursor` query parameter to load older messages; each response contains messages in chronological order.
