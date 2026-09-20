package com.example.KTB_Agile_backend.group.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.group.dto.response.GroupPageResponse;
import com.example.KTB_Agile_backend.group.dto.response.GroupSummary;
import com.example.KTB_Agile_backend.group.entity.Group;
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

	private final GroupRepository groupRepository;

	@Transactional(readOnly = true)
	public GroupPageResponse search(String keyword, String cursor) {
		Long cursorId = decodeCursor(cursor);
		int size = cursorId == null ? INITIAL_PAGE_SIZE : CURSOR_PAGE_SIZE;
		String normalizedKeyword = keyword == null ? "" : keyword.strip();
		Pageable pageable = PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, "id"));
		List<Group> groups = cursorId == null
				? groupRepository.findByDeletedAtIsNullAndGroupNameContainingOrderByIdDesc(
						normalizedKeyword, pageable)
				: groupRepository.findByDeletedAtIsNullAndGroupNameContainingAndIdLessThanOrderByIdDesc(
						normalizedKeyword, cursorId, pageable);

		boolean hasNext = groups.size() > size;
		List<Group> pageGroups = hasNext
				? groups.subList(0, size)
				: groups;
		String nextCursor = hasNext
				? encodeCursor(pageGroups.get(pageGroups.size() - 1).getId())
				: null;

		return new GroupPageResponse(
				pageGroups.stream().map(GroupSummary::from).toList(),
				size,
				hasNext,
				nextCursor
		);
	}

	private static Long decodeCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}

		try {
			long id = Long.parseLong(new String(
					Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
			if (id <= 0) {
				throw new IllegalArgumentException();
			}
			return id;
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.BAD_REQUEST, "cursor가 올바르지 않습니다.");
		}
	}

	private static String encodeCursor(Long groupId) {
		// ponytail: Base64 ID cursor keeps this stateless; sign it if cursor tampering becomes a concern.
		return Base64.getUrlEncoder().withoutPadding().encodeToString(
				String.valueOf(groupId).getBytes(StandardCharsets.UTF_8));
	}
}
