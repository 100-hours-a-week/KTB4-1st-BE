package com.example.KTB_Agile_backend.group.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.common.pagination.CursorCodec;
import com.example.KTB_Agile_backend.group.dto.response.GroupPageResponse;
import com.example.KTB_Agile_backend.group.dto.response.GroupSummary;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupQueryService {

	private static final int INITIAL_PAGE_SIZE = 20;
	private static final int CURSOR_PAGE_SIZE = 10;
	private static final int MY_GROUP_PAGE_SIZE = 5;
	private static final int RECOMMENDATION_PAGE_SIZE = 10;
	private static final int RECOMMENDATION_CURSOR_PARTS = 2;

	private final GroupRepository groupRepository;

	@Transactional(readOnly = true)
	public GroupPageResponse myGroups(Long userId) {
		List<GroupSummary> groups = groupRepository.findMyGroupSummaries(
				userId,
				GroupMemberStatus.ACTIVE,
				PageRequest.of(0, MY_GROUP_PAGE_SIZE)
		);
		return new GroupPageResponse(groups, null, false);
	}

	@Transactional(readOnly = true)
	public GroupPageResponse search(Long userId, String keyword, String cursor) {
		Long cursorId = CursorCodec.decodeId(cursor);
		int size = cursorId == null ? INITIAL_PAGE_SIZE : CURSOR_PAGE_SIZE;
		String normalizedKeyword = keyword == null ? "" : keyword.strip();
		Pageable pageable = PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, "id"));
		List<GroupSummary> groups = cursorId == null
				? groupRepository.findSearchSummaries(
						userId, GroupMemberStatus.ACTIVE, normalizedKeyword, pageable)
				: groupRepository.findSearchSummariesAfter(
						userId, GroupMemberStatus.ACTIVE, normalizedKeyword, cursorId, pageable);

		boolean hasNext = groups.size() > size;
		List<GroupSummary> pageGroups = hasNext
				? groups.subList(0, size)
				: groups;
		String nextCursor = hasNext
				? CursorCodec.encodeId(pageGroups.get(pageGroups.size() - 1).groupId())
				: null;

		return new GroupPageResponse(
				pageGroups,
				nextCursor,
				hasNext
		);
	}

	@Transactional(readOnly = true)
	public GroupPageResponse recommendations(Long userId, String cursor) {
		RecommendationCursor recommendationCursor = decodeRecommendationCursor(cursor);
		Pageable pageable = PageRequest.of(0, RECOMMENDATION_PAGE_SIZE + 1);
		List<GroupSummary> groups = recommendationCursor == null
				? groupRepository.findRecommendations(userId, GroupMemberStatus.ACTIVE, pageable)
				: groupRepository.findRecommendationsAfter(
						userId,
						GroupMemberStatus.ACTIVE,
						recommendationCursor.memberCount(),
						recommendationCursor.groupId(),
						pageable
				);

		boolean hasNext = groups.size() > RECOMMENDATION_PAGE_SIZE;
		List<GroupSummary> pageGroups = hasNext
				? groups.subList(0, RECOMMENDATION_PAGE_SIZE)
				: groups;
		String nextCursor = hasNext
				? encodeRecommendationCursor(pageGroups.get(pageGroups.size() - 1))
				: null;

		return new GroupPageResponse(pageGroups, nextCursor, hasNext);
	}

	private static RecommendationCursor decodeRecommendationCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}

		try {
			String[] values = new String(
					Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split(":", -1);
			if (values.length != RECOMMENDATION_CURSOR_PARTS) {
				throw new IllegalArgumentException();
			}

			long memberCount = Long.parseLong(values[0]);
			long groupId = Long.parseLong(values[1]);
			if (memberCount < 0 || groupId <= 0) {
				throw new IllegalArgumentException();
			}
			return new RecommendationCursor(memberCount, groupId);
		} catch (IllegalArgumentException exception) {
			throw new ApiException(
					ErrorCode.INVALID_CURSOR,
					List.of(),
					exception
			);
		}
	}

	private static String encodeRecommendationCursor(GroupSummary group) {
		String value = group.memberCount() + ":" + group.groupId();
		return Base64.getUrlEncoder().withoutPadding().encodeToString(
				value.getBytes(StandardCharsets.UTF_8));
	}

	private record RecommendationCursor(long memberCount, long groupId) {
	}
}
