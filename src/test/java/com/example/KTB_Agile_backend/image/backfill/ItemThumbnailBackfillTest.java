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
		skipUnsupportedWebpPlatform();
		BufferedImage source = new BufferedImage(1600, 800, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream sourceBytes = new ByteArrayOutputStream();
		ImageIO.write(source, "png", sourceBytes);

		byte[] thumbnail = ItemThumbnailBackfill.createThumbnail(
				sourceBytes.toByteArray(), 1280, "webp", ItemDetailThumbnailBackfill.DETAIL_THUMBNAIL_QUALITY);
		BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));

		assertThat(result.getWidth()).isEqualTo(1280);
		assertThat(result.getHeight()).isEqualTo(640);
	}

	@Test
	void createsThreeWebpVariantsAtExpectedDimensionsAndQualities() throws IOException {
		skipUnsupportedWebpPlatform();
		BufferedImage source = new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream sourceBytes = new ByteArrayOutputStream();
		ImageIO.write(source, "png", sourceBytes);

		byte[] listThumbnail = ItemThumbnailBackfill.createThumbnail(
				sourceBytes.toByteArray(), 224, "webp", ItemThumbnailBackfill.THUMBNAIL_QUALITY);
		byte[] detailThumbnail = ItemThumbnailBackfill.createThumbnail(
				sourceBytes.toByteArray(), 1280, "webp", ItemDetailThumbnailBackfill.DETAIL_THUMBNAIL_QUALITY);
		byte[] original = ItemThumbnailBackfill.createOriginalWebp(
				sourceBytes.toByteArray(), ItemOriginalImageWebpBackfill.ORIGINAL_IMAGE_QUALITY);

		assertThat(ItemThumbnailBackfill.THUMBNAIL_QUALITY).isEqualTo(0.5f);
		assertThat(ItemDetailThumbnailBackfill.DETAIL_THUMBNAIL_QUALITY).isEqualTo(0.6f);
		assertThat(ItemOriginalImageWebpBackfill.ORIGINAL_IMAGE_QUALITY).isEqualTo(0.9f);
		assertDimensions(listThumbnail, 224, 168);
		assertDimensions(detailThumbnail, 1280, 960);
		assertDimensions(original, 4000, 3000);
	}

	@Test
	void originalWebpAppliesExifDisplayOrientation() throws IOException {
		skipUnsupportedWebpPlatform();
		BufferedImage source = new BufferedImage(1600, 800, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream jpegBytes = new ByteArrayOutputStream();
		ImageIO.write(source, "jpeg", jpegBytes);

		byte[] orientedSource = withExifOrientation(jpegBytes.toByteArray(), 6);
		byte[] original = ItemThumbnailBackfill.createOriginalWebp(
				orientedSource, ItemOriginalImageWebpBackfill.ORIGINAL_IMAGE_QUALITY);

		assertDimensions(original, 800, 1600);
	}

	@Test
	void backfillKeysStayStableForCompletedSourcesAndChangeWithTheSourceKey() {
		String sourceKey = "images/42/original.jpg";
		String listKey = ItemThumbnailBackfill.versionedThumbnailKey(42, sourceKey);
		String detailKey = ItemDetailThumbnailBackfill.versionedThumbnailKey(42, sourceKey);
		String originalKey = ItemOriginalImageWebpBackfill.versionedOriginalKey(42, sourceKey);

		assertThat(ItemThumbnailBackfill.versionedThumbnailKey(42, sourceKey)).isEqualTo(listKey);
		assertThat(ItemDetailThumbnailBackfill.versionedThumbnailKey(42, sourceKey)).isEqualTo(detailKey);
		assertThat(ItemOriginalImageWebpBackfill.versionedOriginalKey(42, sourceKey)).isEqualTo(originalKey);
		assertThat(ItemThumbnailBackfill.versionedThumbnailKey(42, originalKey)).isEqualTo(listKey);
		assertThat(ItemDetailThumbnailBackfill.versionedThumbnailKey(42, originalKey)).isEqualTo(detailKey);
		assertThat(ItemThumbnailBackfill.versionedThumbnailKey(42, "images/42/replacement.jpg"))
				.isNotEqualTo(listKey);
		assertThat(ItemDetailThumbnailBackfill.versionedThumbnailKey(42, "images/42/replacement.jpg"))
				.isNotEqualTo(detailKey);
		assertThat(ItemOriginalImageWebpBackfill.versionedOriginalKey(42, "images/42/replacement.jpg"))
				.isNotEqualTo(originalKey);
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

	private static void assertDimensions(byte[] webp, int width, int height) throws IOException {
		assertThat(ItemThumbnailBackfill.isWebp(webp)).isTrue();
		BufferedImage result = ImageIO.read(new ByteArrayInputStream(webp));
		assertThat(result.getWidth()).isEqualTo(width);
		assertThat(result.getHeight()).isEqualTo(height);
	}

	private static byte[] withExifOrientation(byte[] jpeg, int orientation) throws IOException {
		byte[] exif = new byte[] {
				'E', 'x', 'i', 'f', 0, 0, 'I', 'I', 42, 0, 8, 0, 0, 0,
				1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0, (byte) orientation, 0, 0, 0, 0, 0, 0, 0
		};
		int segmentLength = exif.length + 2;
		ByteArrayOutputStream result = new ByteArrayOutputStream(jpeg.length + exif.length + 4);
		result.write(jpeg, 0, 2);
		result.write(0xff);
		result.write(0xe1);
		result.write(segmentLength >>> 8);
		result.write(segmentLength);
		result.write(exif);
		result.write(jpeg, 2, jpeg.length - 2);
		return result.toByteArray();
	}

	private static void skipUnsupportedWebpPlatform() {
		String osName = System.getProperty("os.name");
		String osArchitecture = System.getProperty("os.arch");
		assumeFalse(osName.startsWith("Mac")
				&& ("aarch64".equals(osArchitecture) || "arm64".equals(osArchitecture)),
				"webp-imageio does not bundle a macOS ARM64 native library");
	}
}
