# KTB Final Team Backend

Spring Boot backend project built with Java 25 and Gradle.

## Run

```bash
docker compose up -d --wait mysql
./gradlew bootRun
```

Local execution uses the `dev` profile by default and MySQL 8.4 from Docker Compose on `127.0.0.1:3307`. Flyway applies pending migrations at startup. The dev profile uses a local-only JWT key when `JWT_SECRET` is unset. Tests continue to use an in-memory H2 database.
To select the production profile, set `SPRING_PROFILES_ACTIVE=prod`.

Configure another local database with Spring Boot's datasource environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ktb \
SPRING_DATASOURCE_USERNAME=ktb \
SPRING_DATASOURCE_PASSWORD=change-me \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver \
JPA_DDL_AUTO=validate \
./gradlew bootRun
```

The database must be empty on its first Flyway startup; migrations create the schema.

The `prod` profile requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `AWS_S3_BUCKET`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_REDIRECT_URI`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, and `KAKAO_REDIRECT_URI`. Its schema mode defaults to `validate`.

## Test

```bash
./gradlew test
```

## S3 image flow

Set `AWS_S3_BUCKET`, `AWS_REGION`, and `AI_IMAGE_ANALYSIS_URL`. The AI endpoint receives `POST {"imageUrls":["<url-1>","<url-2>"]}` for 1–3 images. Its JSON response and HTTP status are forwarded to the frontend, including analysis errors. Connection and response timeouts can be set with `AI_CONNECT_TIMEOUT_SECONDS` and `AI_READ_TIMEOUT_SECONDS`. The bucket should remain private; item responses use 1-hour presigned image URLs by default (`AWS_S3_READ_URL_DURATION_SECONDS`), while AI receives a 10-minute URL.

When the user starts analysis, request 1–3 upload URLs with `POST /images/presigned-urls` and `{"images":[{"contentType":"image/jpeg"},{"contentType":"image/png"}]}`. Upload each image to S3 using its `uploadUrl` and `requiredHeaders`, then call `POST /images/ai-analysis` with `{"objectKeys":["<object-key-1>","<object-key-2>"]}`. For an abandoned upload, call `DELETE /images?objectKey=...`. New `POST /items` requests send `objectKeys`; `PUT /items/{itemId}` continues to use `imageIds`.

The client must send the `requiredHeaders` returned by `POST /images/presigned-urls` with its S3 PUT, including when uploading one image. Configure bucket CORS to allow the frontend origin and the `PUT` method with the `Content-Type` and `x-amz-tagging` headers.

## Item text moderation

Set `AI_TEXT_MODERATION_URL` to the AI server's `POST /api/moderation/check-text` URL (the same AI server used by `AI_IMAGE_ANALYSIS_URL`). Before creating an item, call `POST /moderation-checks` with `title` and `content`. The response includes `isAppropriate`, `rejectionReason`, and a five-minute `checkId` when approved. Send that `moderationCheckId` with the item payload to `POST /items`; it is bound to the authenticated user and checked title/content, and can be used once. Inappropriate content returns the AI rejection reason and no `checkId`.

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

In the `dev` and `prod` profiles, Flyway applies versioned migrations before Hibernate validates the schema. Point a fresh local or production database at an empty MySQL schema for the first startup; the database user needs permission to create tables, indexes, and foreign keys. Hibernate uses `ddl-auto=validate` and will not change the schema itself. After V1 has been deployed, add future schema changes as new versioned migrations instead of editing V1.

## WebSocket chat messages

Connect to `/ws` with STOMP `CONNECT` header `Authorization: Bearer <jwt>`. Subscribe to `/topic/chat/rooms/{chatRoomId}` and send `{"content":"안녕하세요"}` to `/app/chat/rooms/{chatRoomId}/messages`. The server persists the message before broadcasting it to room subscribers.

Fetch the latest chat messages with `GET /chat/rooms/{chatRoomId}/messages`. Pass the returned `nextCursor` as the `cursor` query parameter to load older messages; each response contains messages in chronological order.
