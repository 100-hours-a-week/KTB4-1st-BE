package com.example.KTB_Agile_backend.item.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorDetail;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.item.dto.response.ItemLikeResponse;
import com.example.KTB_Agile_backend.item.entity.Item;
import com.example.KTB_Agile_backend.item.entity.ItemLike;
import com.example.KTB_Agile_backend.item.entity.ItemStats;
import com.example.KTB_Agile_backend.item.exception.ItemErrorCode;
import com.example.KTB_Agile_backend.item.repository.ItemLikeRepository;
import com.example.KTB_Agile_backend.item.repository.ItemRepository;
import com.example.KTB_Agile_backend.item.repository.ItemStatsRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemLikeService {

	private static final String ALREADY_LIKED_MESSAGE = "이미 좋아요 누른 게시글입니다.";
	private static final String ITEM_STATS_MISSING_MESSAGE = "좋아요 요청 중 서버 오류가 발생했습니다.";

	private final ItemRepository itemRepository;
	private final ItemStatsRepository itemStatsRepository;
	private final ItemLikeRepository itemLikeRepository;
	private final UserRepository userRepository;

	@Transactional
	public ItemLikeResponse like(Long userId, Long itemId) {
		User user = userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
		Item item = itemRepository.findByIdAndDeletedAtIsNull(itemId)
				.orElseThrow(() -> new ApiException(
						ItemErrorCode.ITEM_NOT_FOUND,
						List.of(new ErrorDetail("itemId", "존재하지 않거나 삭제된 물품입니다."))
				));
		ItemStats stats = itemStatsRepository.findByIdForUpdate(itemId)
				.orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, ITEM_STATS_MISSING_MESSAGE));
		if (itemLikeRepository.findByItemAndUserForUpdate(itemId, userId).isPresent()) {
			throw new ApiException(ErrorCode.CONFLICT, ALREADY_LIKED_MESSAGE);
		}

		ItemLike itemLike = itemLikeRepository.save(new ItemLike(item, user));
		stats.increaseLikeCount();
		return new ItemLikeResponse(itemId, true, stats.getLikeCount(), itemLike.getCreatedAt());
	}

	@Transactional
	public void unlike(Long userId, Long itemId) {
		userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
		itemRepository.findByIdAndDeletedAtIsNull(itemId)
				.orElseThrow(() -> new ApiException(
						ItemErrorCode.ITEM_NOT_FOUND,
						List.of(new ErrorDetail("itemId", "존재하지 않거나 삭제된 물품입니다."))
				));
		ItemStats stats = itemStatsRepository.findByIdForUpdate(itemId)
				.orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, ITEM_STATS_MISSING_MESSAGE));
		ItemLike itemLike = itemLikeRepository.findByItemAndUserForUpdate(itemId, userId).orElse(null);
		if (itemLike == null) {
			return;
		}

		itemLikeRepository.delete(itemLike);
		stats.decreaseLikeCount();
	}
}
