package com.example.KTB_Agile_backend.image.backfill;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.assertj.core.api.Assertions.assertThat;

class ItemThumbnailBackfillTest {

	@Test
	void createsWebpDetailThumbnail() throws IOException {
		String osName = System.getProperty("os.name");
		String osArchitecture = System.getProperty("os.arch");
		assumeFalse(osName.startsWith("Mac")
				&& ("aarch64".equals(osArchitecture) || "arm64".equals(osArchitecture)),
				"webp-imageio does not bundle a macOS ARM64 native library");
		BufferedImage source = new BufferedImage(1600, 800, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream sourceBytes = new ByteArrayOutputStream();
		ImageIO.write(source, "png", sourceBytes);

		byte[] originalWebp = ItemThumbnailBackfill.createThumbnail(
				sourceBytes.toByteArray(), 1600, "webp", 0.8f);
		byte[] thumbnail = ItemThumbnailBackfill.createThumbnail(originalWebp, 1280, "webp", 0.8f);
		BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));

		assertThat(result.getWidth()).isEqualTo(1280);
		assertThat(result.getHeight()).isEqualTo(640);
	}

	@Test
	void scalesToMaxLongEdgeAndFlattensTransparencyToWhite() throws IOException {
		BufferedImage source = new BufferedImage(1600, 800, BufferedImage.TYPE_INT_ARGB);
		ByteArrayOutputStream sourceBytes = new ByteArrayOutputStream();
		ImageIO.write(source, "png", sourceBytes);

		byte[] thumbnail = ItemThumbnailBackfill.createThumbnail(sourceBytes.toByteArray(), 1280, "jpeg", 0.8f);
		BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));

		assertThat(result.getWidth()).isEqualTo(1280);
		assertThat(result.getHeight()).isEqualTo(640);
		assertThat(new Color(result.getRGB(0, 0))).satisfies(color -> {
			assertThat(color.getRed()).isGreaterThan(240);
			assertThat(color.getGreen()).isGreaterThan(240);
			assertThat(color.getBlue()).isGreaterThan(240);
		});
	}

	@Test
	void doesNotUpscaleSmallerImages() throws IOException {
		BufferedImage source = new BufferedImage(640, 320, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream sourceBytes = new ByteArrayOutputStream();
		ImageIO.write(source, "png", sourceBytes);

		byte[] thumbnail = ItemThumbnailBackfill.createThumbnail(sourceBytes.toByteArray(), 1280, "jpeg", 0.8f);
		BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));

		assertThat(result.getWidth()).isEqualTo(640);
		assertThat(result.getHeight()).isEqualTo(320);
	}
}
