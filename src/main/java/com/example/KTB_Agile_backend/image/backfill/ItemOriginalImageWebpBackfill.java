package com.example.KTB_Agile_backend.image.backfill;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ItemOriginalImageWebpBackfill {

	static final float ORIGINAL_IMAGE_QUALITY = 0.9f;
	private static final int BATCH_SIZE = 100;
	private static final int UPDATED_ROW_COUNT = 1;
	private static final System.Logger LOGGER = System.getLogger(ItemOriginalImageWebpBackfill.class.getName());

	private ItemOriginalImageWebpBackfill() {
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
			int awaitingThumbnails = countAwaitingThumbnails(connection);
			long lastImageId = 0L;
			int converted = 0;
			int reused = 0;
			int changedDuringBackfill = 0;
			// shortcut: original keys are not retained; run thumbnails first, or persist source keys for independent reruns.
			while (true) {
				List<Target> batch = findTargets(connection, lastImageId);
				if (batch.isEmpty()) {
					break;
				}
				for (Target target : batch) {
					lastImageId = target.imageId();
					ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
							.bucket(bucket)
							.key(target.objectKey())
							.build());
					byte[] source = response.asByteArray();
					boolean webp = ItemThumbnailBackfill.isWebp(source);
					if (webp && "image/webp".equalsIgnoreCase(response.response().contentType())) {
						reused++;
						continue;
					}

					String originalKey = versionedOriginalKey(target.imageId(), target.objectKey());
					byte[] original = webp
							? source
							: ItemThumbnailBackfill.createOriginalWebp(source, ORIGINAL_IMAGE_QUALITY);
					s3Client.putObject(PutObjectRequest.builder()
							.bucket(bucket)
							.key(originalKey)
							.contentType("image/webp")
							.tagging("pending=false")
							.build(), RequestBody.fromBytes(original));
					if (setObjectKey(connection, target, originalKey) == UPDATED_ROW_COUNT) {
						if (webp) {
							reused++;
						} else {
							converted++;
						}
					} else {
						changedDuringBackfill++;
					}
				}
			}
			LOGGER.log(System.Logger.Level.INFO,
					"Original image backfill complete: {0} encoded, {1} WebP reused, {2} URL-only images skipped, "
							+ "{3} images waiting for thumbnail backfills, {4} changed during backfill",
					converted, reused, urlOnlyImages, awaitingThumbnails, changedDuringBackfill);
		}
	}

	private static List<Target> findTargets(Connection connection, long afterImageId) throws SQLException {
		String sql = """
				select image.image_id, image.object_key, image.thumbnail_object_key,
				       image.detail_thumbnail_object_key,
				       case when item.thumbnail_image_id = image.image_id then 1 else 0 end as is_thumbnail
				from images image
				join items item on item.item_id = image.item_id
				where item.deleted_at is null
				  and image.object_key is not null
				  and image.object_key not like 'images/originals/backfill/v2/%'
				  and image.detail_thumbnail_object_key = concat(
				      'images/detail-thumbnails/backfill/v2/', image.image_id, '-',
				      case when image.object_key like 'images/originals/backfill/v2/%'
				           then substring_index(substring_index(image.object_key, '/', -1), '-', -1)
				           else concat(sha2(image.object_key, 256), '.webp') end)
				  and (item.thumbnail_image_id is null
				       or item.thumbnail_image_id <> image.image_id
				       or image.thumbnail_object_key = concat(
				           'images/thumbnails/backfill/v2/', image.image_id, '-',
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
					targets.add(new Target(
							resultSet.getLong("image_id"),
							resultSet.getString("object_key"),
							resultSet.getString("thumbnail_object_key"),
							resultSet.getString("detail_thumbnail_object_key"),
							resultSet.getBoolean("is_thumbnail")));
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

	private static int countAwaitingThumbnails(Connection connection) throws SQLException {
		String sql = """
				select count(*)
				from images image
				join items item on item.item_id = image.item_id
				where item.deleted_at is null
				  and image.object_key is not null
				  and image.object_key not like 'images/originals/backfill/v2/%'
				  and (image.detail_thumbnail_object_key is null
				       or image.detail_thumbnail_object_key <> concat(
				           'images/detail-thumbnails/backfill/v2/', image.image_id, '-',
				           case when image.object_key like 'images/originals/backfill/v2/%'
				                then substring_index(substring_index(image.object_key, '/', -1), '-', -1)
				                else concat(sha2(image.object_key, 256), '.webp') end)
				       or (item.thumbnail_image_id = image.image_id
				           and (image.thumbnail_object_key is null
				                or image.thumbnail_object_key <> concat(
				                    'images/thumbnails/backfill/v2/', image.image_id, '-',
				                    case when image.object_key like 'images/originals/backfill/v2/%'
				                         then substring_index(substring_index(image.object_key, '/', -1), '-', -1)
				                         else concat(sha2(image.object_key, 256), '.webp') end))))
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql);
			 ResultSet resultSet = statement.executeQuery()) {
			return resultSet.next() ? resultSet.getInt(1) : 0;
		}
	}

	static String versionedOriginalKey(long imageId, String objectKey) {
		return ItemImageBackfillKeys.originalKey(imageId, objectKey);
	}

	private static int setObjectKey(Connection connection, Target target, String objectKey) throws SQLException {
		String sql = """
				update images
				set object_key = ?
				where image_id = ?
				  and object_key = ?
				  and (thumbnail_object_key = ? or (thumbnail_object_key is null and ? is null))
				  and detail_thumbnail_object_key = ?
				  and exists (
				      select 1 from items
				      where items.item_id = images.item_id
				        and items.deleted_at is null
				        and ((? = 1 and items.thumbnail_image_id = images.image_id)
				             or (? = 0 and (items.thumbnail_image_id is null
				                            or items.thumbnail_image_id <> images.image_id)))
				  )
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, objectKey);
			statement.setLong(2, target.imageId());
			statement.setString(3, target.objectKey());
			statement.setString(4, target.thumbnailObjectKey());
			statement.setString(5, target.thumbnailObjectKey());
			statement.setString(6, target.detailThumbnailObjectKey());
			statement.setInt(7, target.thumbnail() ? 1 : 0);
			statement.setInt(8, target.thumbnail() ? 1 : 0);
			return statement.executeUpdate();
		}
	}

	private static String requiredEnvironmentVariable(String name) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Required environment variable is not set: " + name);
		}
		return value;
	}

	private record Target(
			long imageId,
			String objectKey,
			String thumbnailObjectKey,
			String detailThumbnailObjectKey,
			boolean thumbnail
	) {
	}
}
