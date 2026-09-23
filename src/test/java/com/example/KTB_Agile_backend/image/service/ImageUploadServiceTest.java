package com.example.KTB_Agile_backend.image.service;

import com.example.KTB_Agile_backend.image.dto.request.PresignedUploadRequest;
import com.example.KTB_Agile_backend.image.dto.response.PresignedUploadResponse;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUploadServiceTest {

	@Test
	void issuesPresignedPutUrlWithoutCallingS3() {
		try (S3Presigner presigner = S3Presigner.builder()
				.region(Region.AP_NORTHEAST_2)
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create("test-access-key", "test-secret-key")))
				.build()) {
			ImageUploadService service = new ImageUploadService(
					presigner, "test-bucket", 600);

			PresignedUploadResponse response = service.issue(
					42L, new PresignedUploadRequest("image/jpeg"));

			assertThat(response.uploadUrl()).contains("test-bucket", "X-Amz-Signature");
		}
	}

	@Test
	void issuesOnePresignedPutUrlPerImage() {
		try (S3Presigner presigner = S3Presigner.builder()
				.region(Region.AP_NORTHEAST_2)
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create("test-access-key", "test-secret-key")))
				.build()) {
			ImageUploadService service = new ImageUploadService(presigner, "test-bucket", 600);

			List<PresignedUploadResponse> responses = service.issue(42L, List.of(
					new PresignedUploadRequest("image/jpeg"),
					new PresignedUploadRequest("image/png")
			));

			assertThat(responses).hasSize(2)
					.allSatisfy(response -> assertThat(response.uploadUrl()).contains("test-bucket", "X-Amz-Signature"));
			assertThat(responses.get(0).objectKey()).isNotEqualTo(responses.get(1).objectKey());
		}
	}
}
