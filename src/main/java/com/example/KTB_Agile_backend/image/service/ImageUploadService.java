package com.example.KTB_Agile_backend.image.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.image.exception.ImageErrorCode;
import com.example.KTB_Agile_backend.image.dto.request.PresignedUploadRequest;
import com.example.KTB_Agile_backend.image.dto.response.PresignedUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageUploadService {

	private static final String OBJECT_KEY_PREFIX = "images/";
	private static final Map<String, String> FILE_EXTENSIONS = Map.of(
			"image/jpeg", ".jpg",
			"image/png", ".png",
			"image/webp", ".webp"
	);

	private final S3Presigner s3Presigner;
	private final String bucket;
	private final Duration presignedUrlDuration;

	public ImageUploadService(
			S3Presigner s3Presigner,
			@Value("${cloud.aws.s3.bucket}") String bucket,
			@Value("${cloud.aws.s3.presigned-url-duration-seconds:600}") long durationSeconds
	) {
		if (durationSeconds <= 0 || durationSeconds > Duration.ofDays(7).toSeconds()) {
			throw new IllegalArgumentException("S3 presigned URL duration must be between 1 and 604800 seconds");
		}
		this.s3Presigner = s3Presigner;
		this.bucket = bucket;
		this.presignedUrlDuration = Duration.ofSeconds(durationSeconds);
	}

	public PresignedUploadResponse issue(Long userId, PresignedUploadRequest request) {
		if (bucket.isBlank()) {
			throw new ApiException(ImageErrorCode.S3_BUCKET_NOT_CONFIGURED);
		}
		String extension = FILE_EXTENSIONS.get(request.contentType());
		if (extension == null) {
			throw new ApiException(ImageErrorCode.UNSUPPORTED_IMAGE_TYPE);
		}

		String objectKey = OBJECT_KEY_PREFIX + userId + "/"
				+ UUID.randomUUID() + extension;
		// ponytail: PUT presign does not enforce a size ceiling; use a POST policy when limits are required.
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(objectKey)
				.contentType(request.contentType())
				.tagging("pending=true")
				.build();
		PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(builder -> builder
				.signatureDuration(presignedUrlDuration)
				.putObjectRequest(putObjectRequest));

		return new PresignedUploadResponse(
				presignedRequest.url().toString(),
				objectKey,
				presignedUrlDuration.toSeconds(),
				Map.of(
						"Content-Type", request.contentType(),
						"x-amz-tagging", "pending=true"
				)
		);
	}

	public List<PresignedUploadResponse> issue(Long userId, List<PresignedUploadRequest> requests) {
		return requests.stream().map(request -> issue(userId, request)).toList();
	}
}
