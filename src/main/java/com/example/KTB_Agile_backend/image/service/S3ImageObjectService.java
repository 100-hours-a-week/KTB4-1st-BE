package com.example.KTB_Agile_backend.image.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.image.exception.ImageErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

@Service
public class S3ImageObjectService {

	private static final String OBJECT_KEY_PREFIX = "images/";
	private static final String PENDING_TAG_KEY = "pending";
	private static final String PENDING_TAG_VALUE = "true";
	private static final CloudFrontUtilities CLOUDFRONT_UTILITIES = CloudFrontUtilities.create();
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final String bucket;
	private final Duration presignedUrlDuration;
	private final Duration readUrlDuration;
	private final String cloudFrontDomain;
	private final CannedSignerRequest cloudFrontSignerRequest;

	@SuppressWarnings("PMD.NullAssignment")
	public S3ImageObjectService(
			S3Client s3Client,
			S3Presigner s3Presigner,
			@Value("${cloud.aws.s3.bucket}") String bucket,
			@Value("${cloud.aws.s3.presigned-url-duration-seconds:600}") long durationSeconds,
			@Value("${cloud.aws.s3.read-url-duration-seconds:3600}") long readUrlDurationSeconds,
			@Value("${cloud.aws.cloudfront.domain:}") String cloudFrontDomain,
			@Value("${cloud.aws.cloudfront.public-key-id:}") String cloudFrontPublicKeyId,
			@Value("${cloud.aws.cloudfront.private-key-pem:}") String cloudFrontPrivateKeyPem
	) {
		if (readUrlDurationSeconds <= 0 || readUrlDurationSeconds > Duration.ofDays(7).toSeconds()) {
			throw new IllegalArgumentException("S3 read URL duration must be between 1 and 604800 seconds");
		}
		boolean cloudFrontDisabled = cloudFrontDomain.isBlank()
				&& cloudFrontPublicKeyId.isBlank()
				&& cloudFrontPrivateKeyPem.isBlank();
		boolean cloudFrontConfigured = !cloudFrontDomain.isBlank()
				&& !cloudFrontPublicKeyId.isBlank()
				&& !cloudFrontPrivateKeyPem.isBlank();
		if (!cloudFrontDisabled && !cloudFrontConfigured) {
			throw new IllegalArgumentException(
					"CloudFront domain, public key ID, and private key PEM must be configured together");
		}
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.bucket = bucket;
		this.presignedUrlDuration = Duration.ofSeconds(durationSeconds);
		this.readUrlDuration = Duration.ofSeconds(readUrlDurationSeconds);
		this.cloudFrontDomain = cloudFrontDomain;
		this.cloudFrontSignerRequest = cloudFrontConfigured
				? loadCloudFrontSignerRequest(cloudFrontDomain, cloudFrontPublicKeyId, cloudFrontPrivateKeyPem)
				: null;
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
				throw new ApiException(ImageErrorCode.IMAGE_NOT_PENDING);
			}
		} catch (S3Exception exception) {
			throw s3Failure(exception);
		}
	}

	public String presignedReadUrl(String objectKey) {
		return signedUrl(objectKey, readUrlDuration);
	}

	public String presignedAnalysisUrl(String objectKey) {
		return signedUrl(objectKey, presignedUrlDuration);
	}

	private String signedUrl(String objectKey, Duration duration) {
		if (cloudFrontSignerRequest == null) {
			return presignedGetUrl(objectKey, duration);
		}
		return CLOUDFRONT_UTILITIES.getSignedUrlWithCannedPolicy(cloudFrontSignerRequest.toBuilder()
				.resourceUrl("https://" + cloudFrontDomain + "/" + objectKey)
				.expirationDate(Instant.now().plus(duration))
				.build())
			.url();
	}

	private static CannedSignerRequest loadCloudFrontSignerRequest(
			String domain,
			String publicKeyId,
			String privateKeyPem
	) {
		try {
			// The AWS SDK's public signer API reads PEM private keys from a file path.
			Path keyFile = Files.createTempFile("cloudfront-key-", ".pem",
					PosixFilePermissions.asFileAttribute(EnumSet.of(
							PosixFilePermission.OWNER_READ,
							PosixFilePermission.OWNER_WRITE)));
			try {
				Files.writeString(keyFile, privateKeyPem.replace("\\n", "\n"));
				return CannedSignerRequest.builder()
						.resourceUrl("https://" + domain + "/")
						.privateKey(keyFile)
						.keyPairId(publicKeyId)
						.expirationDate(Instant.now())
						.build();
			} finally {
				Files.deleteIfExists(keyFile);
			}
		} catch (Exception exception) {
			throw new IllegalArgumentException("Unable to load CloudFront private key PEM", exception);
		}
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
				throw new ApiException(ImageErrorCode.IMAGE_NOT_PENDING);
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
			throw new ApiException(ImageErrorCode.IMAGE_NOT_OWNED);
		}
	}

	private void requireBucket() {
		if (bucket.isBlank()) {
			throw new ApiException(ImageErrorCode.S3_BUCKET_NOT_CONFIGURED);
		}
	}

	private static ApiException s3Failure(S3Exception exception) {
		if (HttpStatus.NOT_FOUND.value() == exception.statusCode()) {
			return new ApiException(ImageErrorCode.S3_IMAGE_NOT_FOUND, List.of(), exception);
		}
		return new ApiException(ImageErrorCode.S3_IMAGE_PROCESSING_FAILED, List.of(), exception);
	}
}
