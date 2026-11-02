package com.example.KTB_Agile_backend.image.backfill;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ItemThumbnailBackfill {

	private static final int THUMBNAIL_MAX_SIZE = 224;
	private static final float JPEG_QUALITY = 0.8f;
	private static final int BATCH_SIZE = 100;
	private static final String THUMBNAIL_KEY_PREFIX = "images/thumbnails/backfill/";

	private ItemThumbnailBackfill() {
	}

	public static void main(String[] args) throws Exception {
		String dbUrl = requiredEnvironmentVariable("DB_URL");
		String dbUsername = requiredEnvironmentVariable("DB_USERNAME");
		String dbPassword = requiredEnvironmentVariable("DB_PASSWORD");
		String bucket = requiredEnvironmentVariable("AWS_S3_BUCKET");
		String region = System.getenv().getOrDefault("AWS_REGION", "ap-northeast-2");

		try (Connection connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
			 S3Client s3Client = S3Client.builder().region(Region.of(region)).build()) {
			int urlOnlyImages = countUrlOnlyImages(connection);
			long lastImageId = 0L;
			int processed = 0;
			while (true) {
				List<Target> batch = findTargets(connection, lastImageId);
				if (batch.isEmpty()) {
					break;
				}
				for (Target target : batch) {
					lastImageId = target.imageId();
					byte[] source = s3Client.getObjectAsBytes(GetObjectRequest.builder()
							.bucket(bucket)
							.key(target.objectKey())
							.build()).asByteArray();
					byte[] thumbnail = createThumbnail(source);
					String thumbnailKey = THUMBNAIL_KEY_PREFIX + target.imageId() + ".jpg";
					s3Client.putObject(PutObjectRequest.builder()
							.bucket(bucket)
							.key(thumbnailKey)
							.contentType("image/jpeg")
							.tagging("pending=false")
							.build(), RequestBody.fromBytes(thumbnail));
					if (setThumbnailKey(connection, target.imageId(), thumbnailKey) == 1) {
						processed++;
					}
					if (processed > 0 && processed % BATCH_SIZE == 0) {
						System.out.printf("Backfilled %d thumbnails (through image %d)%n", processed, lastImageId);
					}
				}
			}
			System.out.printf("Backfill complete: %d thumbnails created, %d URL-only images skipped%n",
					processed, urlOnlyImages);
		}
	}

	private static List<Target> findTargets(Connection connection, long afterImageId) throws SQLException {
		// shortcut: S3-backed rows only; add allowlisted URL reads if production contains URL-only images.
		String sql = """
				select image.image_id, image.object_key
				from images image
				join items item on item.thumbnail_image_id = image.image_id
				where item.deleted_at is null
				  and image.thumbnail_object_key is null
				  and image.object_key is not null
				  and image.image_id > ?
				order by image.image_id
				limit ?
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, afterImageId);
			statement.setInt(2, BATCH_SIZE);
			try (ResultSet resultSet = statement.executeQuery()) {
				List<Target> targets = new ArrayList<>();
				while (resultSet.next()) {
					targets.add(new Target(resultSet.getLong("image_id"), resultSet.getString("object_key")));
				}
				return targets;
			}
		}
	}

	private static int countUrlOnlyImages(Connection connection) throws SQLException {
		String sql = """
				select count(*)
				from images image
				join items item on item.thumbnail_image_id = image.image_id
				where item.deleted_at is null
				  and image.thumbnail_object_key is null
				  and image.object_key is null
				  and image.image_url is not null
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql);
			 ResultSet resultSet = statement.executeQuery()) {
			return resultSet.next() ? resultSet.getInt(1) : 0;
		}
	}

	private static int setThumbnailKey(Connection connection, long imageId, String thumbnailKey) throws SQLException {
		String sql = """
				update images
				set thumbnail_object_key = ?
				where image_id = ?
				  and thumbnail_object_key is null
				  and exists (
				      select 1 from items where items.thumbnail_image_id = images.image_id
				  )
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, thumbnailKey);
			statement.setLong(2, imageId);
			return statement.executeUpdate();
		}
	}

	private static byte[] createThumbnail(byte[] sourceBytes) throws IOException {
		int orientation = readOrientation(sourceBytes);
		BufferedImage source = decode(sourceBytes);
		BufferedImage oriented = orient(source, orientation);
		int sourceMaxSize = Math.max(oriented.getWidth(), oriented.getHeight());
		double scale = Math.min(1.0, (double) THUMBNAIL_MAX_SIZE / sourceMaxSize);
		int width = Math.max(1, (int) Math.round(oriented.getWidth() * scale));
		int height = Math.max(1, (int) Math.round(oriented.getHeight() * scale));
		if (Math.max(width, height) > THUMBNAIL_MAX_SIZE) {
			throw new IOException("Thumbnail exceeds the configured maximum size");
		}

		BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = thumbnail.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, width, height);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		graphics.drawImage(oriented, 0, 0, width, height, null);
		graphics.dispose();

		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
		if (!writers.hasNext()) {
			throw new IOException("No JPEG encoder is available");
		}
		ImageWriter writer = writers.next();
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			 ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
			writer.setOutput(output);
			ImageWriteParam parameters = writer.getDefaultWriteParam();
			parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			parameters.setCompressionQuality(JPEG_QUALITY);
			writer.write(null, new IIOImage(thumbnail, null, null), parameters);
			output.flush();
			return bytes.toByteArray();
		} finally {
			writer.dispose();
		}
	}

	private static BufferedImage decode(byte[] sourceBytes) throws IOException {
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(sourceBytes))) {
			if (input == null) {
				throw new IOException("Unable to read image data");
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw new IOException("Unsupported image format");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				int maxDimension = Math.max(reader.getWidth(0), reader.getHeight(0));
				int sampling = Math.max(1, maxDimension / THUMBNAIL_MAX_SIZE);
				ImageReadParam parameters = reader.getDefaultReadParam();
				parameters.setSourceSubsampling(sampling, sampling, 0, 0);
				BufferedImage image = reader.read(0, parameters);
				if (image == null) {
					throw new IOException("Image decoder returned no image");
				}
				return image;
			} finally {
				reader.dispose();
			}
		}
	}

	private static int readOrientation(byte[] sourceBytes) {
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(sourceBytes));
			ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			return directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)
					? directory.getInt(ExifIFD0Directory.TAG_ORIENTATION)
					: 1;
		} catch (ImageProcessingException | MetadataException | IOException exception) {
			return 1;
		}
	}

	private static BufferedImage orient(BufferedImage source, int orientation) {
		int width = source.getWidth();
		int height = source.getHeight();
		boolean swapDimensions = orientation >= 5 && orientation <= 8;
		int outputWidth = swapDimensions ? height : width;
		int outputHeight = swapDimensions ? width : height;
		AffineTransform transform = switch (orientation) {
			case 2 -> new AffineTransform(-1, 0, 0, 1, width - 1, 0);
			case 3 -> new AffineTransform(-1, 0, 0, -1, width - 1, height - 1);
			case 4 -> new AffineTransform(1, 0, 0, -1, 0, height - 1);
			case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
			case 6 -> new AffineTransform(0, 1, -1, 0, height - 1, 0);
			case 7 -> new AffineTransform(0, -1, -1, 0, height - 1, width - 1);
			case 8 -> new AffineTransform(0, -1, 1, 0, 0, width - 1);
			default -> new AffineTransform();
		};
		if (orientation < 2 || orientation > 8) {
			return source;
		}
		BufferedImage oriented = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = oriented.createGraphics();
		graphics.drawImage(source, transform, null);
		graphics.dispose();
		return oriented;
	}

	private static String requiredEnvironmentVariable(String name) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Required environment variable is not set: " + name);
		}
		return value;
	}

	private record Target(long imageId, String objectKey) {
	}
}
