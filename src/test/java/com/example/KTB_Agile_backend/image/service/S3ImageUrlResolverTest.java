package com.example.KTB_Agile_backend.image.service;

import com.example.KTB_Agile_backend.image.entity.Image;
import com.example.KTB_Agile_backend.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class S3ImageUrlResolverTest {

	@Test
	void resolvesDetailThumbnailUrlAndFallsBackToOriginal() {
		S3ImageObjectService s3ImageObjectService = mock(S3ImageObjectService.class);
		when(s3ImageObjectService.presignedReadUrl("images/42/detail.webp"))
				.thenReturn("https://example.com/detail.webp");
		when(s3ImageObjectService.presignedReadUrl("images/42/legacy.webp"))
				.thenReturn("https://example.com/legacy.webp");
		S3ImageUrlResolver resolver = new S3ImageUrlResolver(s3ImageObjectService);

		Image image = Image.fromS3Object(mock(User.class), "images/42/original.webp");
		image.setDetailThumbnailObjectKey("images/42/detail.webp");
		Image legacyImage = Image.fromS3Object(mock(User.class), "images/42/legacy.webp");

		assertThat(resolver.resolveDetailThumbnail(image)).isEqualTo("https://example.com/detail.webp");
		assertThat(resolver.resolveDetailThumbnail(legacyImage)).isEqualTo("https://example.com/legacy.webp");
	}
}
