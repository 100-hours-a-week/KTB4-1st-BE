# KTB Final Team Backend

Spring Boot backend project built with Java 25 and Gradle.

## Run

```bash
./gradlew bootRun
```

Local execution uses the `dev` profile by default and an in-memory H2 database.
To select the production profile, set `SPRING_PROFILES_ACTIVE=prod`.

Configure another local database with Spring Boot's datasource environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ktb \
SPRING_DATASOURCE_USERNAME=ktb \
SPRING_DATASOURCE_PASSWORD=change-me \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver \
JPA_DDL_AUTO=update \
./gradlew bootRun
```

The `prod` profile requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `AWS_S3_BUCKET`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_REDIRECT_URI`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, and `KAKAO_REDIRECT_URI`. Its schema mode defaults to `validate`.

## Test

```bash
./gradlew test
```

## S3 image flow

Set `AWS_S3_BUCKET`, `AWS_REGION`, and `AI_IMAGE_ANALYSIS_URL`. The AI endpoint receives `POST {"imageUrls":["<url-1>","<url-2>"]}` for 1–3 images. Its JSON response and HTTP status are forwarded to the frontend, including analysis errors. Connection and response timeouts can be set with `AI_CONNECT_TIMEOUT_SECONDS` and `AI_READ_TIMEOUT_SECONDS`. The bucket should remain private; item responses use 1-hour presigned image URLs by default (`AWS_S3_READ_URL_DURATION_SECONDS`), while AI receives a 10-minute URL.

When the user starts analysis, request 1–3 upload URLs with `POST /images/presigned-urls` and `{"images":[{"contentType":"image/jpeg"},{"contentType":"image/png"}]}`. Upload each image to S3 using its `uploadUrl` and `requiredHeaders`, then call `POST /images/ai-analysis` with `{"objectKeys":["<object-key-1>","<object-key-2>"]}`. For an abandoned upload, call `DELETE /images?objectKey=...`. New `POST /items` requests send `objectKeys`; `PUT /items/{itemId}` continues to use `imageIds`.

The client must send the `requiredHeaders` returned by `POST /images/presigned-urls` with its S3 PUT, including when uploading one image. Configure bucket CORS to allow the frontend origin and the `PUT` method with the `Content-Type` and `x-amz-tagging` headers.

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

For an existing MySQL database, apply this schema change before deploying:

```sql
ALTER TABLE images
    ADD COLUMN object_key VARCHAR(512) NULL,
    MODIFY COLUMN image_url TEXT NULL;

CREATE UNIQUE INDEX uk_images_object_key ON images (object_key);
```

Existing URL-backed image rows continue to work; newly uploaded images store their S3 key and receive fresh read URLs from item APIs.
