package com.example.KTB_Agile_backend.image.backfill;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ItemDetailThumbnailBackfill {

	private static final int DETAIL_THUMBNAIL_MAX_SIZE = 1280;
	static final float DETAIL_THUMBNAIL_QUALITY = 0.6f;
	private static final int BATCH_SIZE = 100;
	private static final int UPDATED_ROW_COUNT = 1;
	private static final System.Logger LOGGER = System.getLogger(ItemDetailThumbnailBackfill.class.getName());

	private ItemDetailThumbnailBackfill() {
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
					byte[] thumbnail = ItemThumbnailBackfill.createThumbnail(
							source, DETAIL_THUMBNAIL_MAX_SIZE, "webp", DETAIL_THUMBNAIL_QUALITY);
					String thumbnailKey = versionedThumbnailKey(target.imageId(), target.objectKey());
					s3Client.putObject(PutObjectRequest.builder()
							.bucket(bucket)
							.key(thumbnailKey)
							.contentType("image/webp")
							.tagging("pending=false")
							.build(), RequestBody.fromBytes(thumbnail));
					if (setDetailThumbnailKey(connection, target, thumbnailKey) == UPDATED_ROW_COUNT) {
						processed++;
					}
					if (processed > 0 && processed % BATCH_SIZE == 0) {
						LOGGER.log(System.Logger.Level.INFO,
								"Backfilled {0} detail thumbnails (through image {1})", processed, lastImageId);
					}
				}
			}
			LOGGER.log(System.Logger.Level.INFO,
					"Detail thumbnail backfill complete: {0} images created, {1} URL-only images skipped",
					processed, urlOnlyImages);
		}
	}

	private static List<Target> findTargets(Connection connection, long afterImageId) throws SQLException {
		String sql = """
				select image.image_id, image.object_key, image.detail_thumbnail_object_key
				from images image
				join items item on item.item_id = image.item_id
				where item.deleted_at is null
				  and image.object_key is not null
				  and (image.detail_thumbnail_object_key is null
				       or image.detail_thumbnail_object_key <> concat(
				           'images/detail-thumbnails/backfill/v2/', image.image_id, '-',
				           case when image.object_key like 'images/originals/backfill/v2/%'
				                then substring_index(substring_index(image.object_key, '/', -1), '-', -1)
				                else concat(sha2(image.object_key, 256), '.webp') end))
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
					targets.add(new Target(resultSet.getLong("image_id"), resultSet.getString("object_key"),
							resultSet.getString("detail_thumbnail_object_key")));
				}
				return targets;
			}
		}
	}

	private static int countUrlOnlyImages(Connection connection) throws SQLException {
		String sql = """
				select count(*)
				from images image
				join items item on item.item_id = image.item_id
				where item.deleted_at is null
				  and image.object_key is null
				  and image.image_url is not null
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql);
			 ResultSet resultSet = statement.executeQuery()) {
			return resultSet.next() ? resultSet.getInt(1) : 0;
		}
	}

	private static int setDetailThumbnailKey(Connection connection, Target target, String thumbnailKey)
			throws SQLException {
		String sql = """
				update images
				set detail_thumbnail_object_key = ?
				where image_id = ?
				  and object_key = ?
				  and (detail_thumbnail_object_key = ? or (detail_thumbnail_object_key is null and ? is null))
				  and exists (
				      select 1 from items
				      where items.item_id = images.item_id and items.deleted_at is null
				  )
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, thumbnailKey);
			statement.setLong(2, target.imageId());
			statement.setString(3, target.objectKey());
			statement.setString(4, target.detailThumbnailObjectKey());
			statement.setString(5, target.detailThumbnailObjectKey());
			return statement.executeUpdate();
		}
	}

	static String versionedThumbnailKey(long imageId, String objectKey) {
		return ItemImageBackfillKeys.detailThumbnailKey(imageId, objectKey);
	}

	private static String requiredEnvironmentVariable(String name) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Required environment variable is not set: " + name);
		}
		return value;
	}

	private record Target(long imageId, String objectKey, String detailThumbnailObjectKey) {
	}
}
