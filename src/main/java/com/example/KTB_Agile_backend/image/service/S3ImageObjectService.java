package com.example.KTB_Agile_backend.image.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

@Service
public class S3ImageObjectService {

	private static final String OBJECT_KEY_PREFIX = "images/";
	private static final String PENDING_TAG_KEY = "pending";
	private static final String PENDING_TAG_VALUE = "true";
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final String bucket;
	private final Duration presignedUrlDuration;
	private final Duration readUrlDuration;

	public S3ImageObjectService(
			S3Client s3Client,
			S3Presigner s3Presigner,
			@Value("${cloud.aws.s3.bucket}") String bucket,
			@Value("${cloud.aws.s3.presigned-url-duration-seconds:600}") long durationSeconds,
			@Value("${cloud.aws.s3.read-url-duration-seconds:3600}") long readUrlDurationSeconds
	) {
		if (readUrlDurationSeconds <= 0 || readUrlDurationSeconds > Duration.ofDays(7).toSeconds()) {
			throw new IllegalArgumentException("S3 read URL duration must be between 1 and 604800 seconds");
		}
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.bucket = bucket;
		this.presignedUrlDuration = Duration.ofSeconds(durationSeconds);
		this.readUrlDuration = Duration.ofSeconds(readUrlDurationSeconds);
	}

	public void validatePendingObjects(Long userId, Collection<String> objectKeys) {
		for (String objectKey : objectKeys) {
			validatePendingObject(userId, objectKey);
		}
	}

	public void validatePendingObject(Long userId, String objectKey) {
		requireOwnedKey(userId, objectKey);
		requireBucket();
		try {
			s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
			boolean pending = s3Client.getObjectTagging(GetObjectTaggingRequest.builder()
					.bucket(bucket)
					.key(objectKey)
					.build())
					.tagSet()
					.stream()
					.anyMatch(tag -> PENDING_TAG_KEY.equals(tag.key()) && PENDING_TAG_VALUE.equals(tag.value()));
			if (!pending) {
				throw new ApiException(ErrorCode.CONFLICT, "미등록 상태의 이미지가 아닙니다.");
			}
		} catch (S3Exception exception) {
			throw s3Failure(exception);
		}
	}

	public String presignedReadUrl(String objectKey) {
		return presignedGetUrl(objectKey, readUrlDuration);
	}

	public String presignedAnalysisUrl(String objectKey) {
		return presignedGetUrl(objectKey, presignedUrlDuration);
	}

	private String presignedGetUrl(String objectKey, Duration duration) {
		requireBucket();
		return s3Presigner.presignGetObject(builder -> builder
				.signatureDuration(duration)
				.getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(objectKey).build()))
				.url()
				.toString();
	}

	public void markRegistered(Collection<String> objectKeys) {
		setPendingTag(objectKeys, "false");
	}

	private void setPendingTag(Collection<String> objectKeys, String value) {
		requireBucket();
		for (String objectKey : objectKeys) {
			try {
				s3Client.putObjectTagging(PutObjectTaggingRequest.builder()
						.bucket(bucket)
						.key(objectKey)
						.tagging(Tagging.builder()
								.tagSet(List.of(Tag.builder().key(PENDING_TAG_KEY).value(value).build()))
								.build())
						.build());
			} catch (S3Exception exception) {
				throw s3Failure(exception);
			}
		}
	}

	public void deletePendingObject(Long userId, String objectKey) {
		requireOwnedKey(userId, objectKey);
		requireBucket();
		try {
			boolean pending = s3Client.getObjectTagging(GetObjectTaggingRequest.builder()
					.bucket(bucket)
					.key(objectKey)
					.build())
				.tagSet()
				.stream()
				.anyMatch(tag -> PENDING_TAG_KEY.equals(tag.key()) && PENDING_TAG_VALUE.equals(tag.value()));
			if (!pending) {
				throw new ApiException(ErrorCode.CONFLICT, "미등록 상태의 이미지가 아닙니다.");
			}
			s3Client.deleteObject(builder -> builder.bucket(bucket).key(objectKey));
		} catch (S3Exception exception) {
			if (HttpStatus.NOT_FOUND.value() == exception.statusCode()) {
				return;
			}
			throw s3Failure(exception);
		}
	}

	private void requireOwnedKey(Long userId, String objectKey) {
		if (objectKey == null || !objectKey.startsWith(OBJECT_KEY_PREFIX + userId + "/")) {
			throw new ApiException(ErrorCode.FORBIDDEN, "소유하지 않은 이미지입니다.");
		}
	}

	private void requireBucket() {
		if (bucket.isBlank()) {
			throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "S3 버킷이 설정되지 않았습니다.");
		}
	}

	private static ApiException s3Failure(S3Exception exception) {
		if (HttpStatus.NOT_FOUND.value() == exception.statusCode()) {
			return new ApiException(ErrorCode.NOT_FOUND, "S3에서 이미지를 찾을 수 없습니다.", exception);
		}
		return new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "S3 이미지 처리에 실패했습니다.", exception);
	}
}
