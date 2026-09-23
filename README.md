# KTB Final Team Backend

Spring Boot backend project built with Java 25 and Gradle.

## Run

```bash
./gradlew bootRun
```

The default local database is an in-memory H2 database. Configure another database with:

```bash
DB_URL=jdbc:mysql://localhost:3306/ktb \
DB_USERNAME=ktb \
DB_PASSWORD=change-me \
DB_DRIVER=com.mysql.cj.jdbc.Driver \
JPA_DDL_AUTO=update \
./gradlew bootRun
```

## Test

```bash
./gradlew test
```

## S3 image flow

Set `AWS_S3_BUCKET`, `AWS_REGION`, and `AI_IMAGE_ANALYSIS_URL`. The AI endpoint receives `POST {"imageUrl":"<presigned GET URL>"}`. Connection and response timeouts can be set with `AI_CONNECT_TIMEOUT_SECONDS` and `AI_READ_TIMEOUT_SECONDS`. The bucket should remain private; item responses use 1-hour presigned image URLs by default (`AWS_S3_READ_URL_DURATION_SECONDS`), while AI receives a 10-minute URL.

After upload, call `POST /images/ai-analysis` with `{"objectKey":"..."}`. For an abandoned upload, call `DELETE /images?objectKey=...`. New `POST /items` requests send `objectKeys`; `PUT /items/{itemId}` continues to use `imageIds`.

The client must send the `requiredHeaders` returned by `POST /images/presigned-url` with its S3 PUT. Configure bucket CORS to allow the frontend origin and the `PUT` method with the `Content-Type` and `x-amz-tagging` headers.

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
