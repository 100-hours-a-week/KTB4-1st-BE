package com.example.KTB_Agile_backend.image.backfill;

import com.example.KTB_Agile_backend.common.util.Hashing;

final class ItemImageBackfillKeys {

	static final String ORIGINAL_PREFIX = "images/originals/backfill/v2/";
	static final String THUMBNAIL_PREFIX = "images/thumbnails/backfill/v2/";
	static final String DETAIL_THUMBNAIL_PREFIX = "images/detail-thumbnails/backfill/v2/";
	private static final String WEBP_EXTENSION = ".webp";
	private static final int SHA256_HEX_LENGTH = 64;

	private ItemImageBackfillKeys() {
	}

	static String sourceHash(String objectKey) {
		if (objectKey.startsWith(ORIGINAL_PREFIX) && objectKey.endsWith(WEBP_EXTENSION)) {
			int separator = objectKey.lastIndexOf('-');
			int hashEnd = objectKey.length() - WEBP_EXTENSION.length();
			if (hashEnd - separator - 1 == SHA256_HEX_LENGTH) {
				return objectKey.substring(separator + 1, hashEnd);
			}
		}
		return Hashing.sha256(objectKey);
	}

	static String thumbnailKey(long imageId, String objectKey) {
		return THUMBNAIL_PREFIX + imageId + "-" + sourceHash(objectKey) + WEBP_EXTENSION;
	}

	static String detailThumbnailKey(long imageId, String objectKey) {
		return DETAIL_THUMBNAIL_PREFIX + imageId + "-" + sourceHash(objectKey) + WEBP_EXTENSION;
	}

	static String originalKey(long imageId, String objectKey) {
		return ORIGINAL_PREFIX + imageId + "-" + Hashing.sha256(objectKey) + WEBP_EXTENSION;
	}
}
