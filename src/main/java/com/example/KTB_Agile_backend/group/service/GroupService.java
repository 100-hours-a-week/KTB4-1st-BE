package com.example.KTB_Agile_backend.group.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.group.exception.GroupErrorCode;
import com.example.KTB_Agile_backend.group.dto.request.CreateGroupRequest;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GroupService {

	private static final int MAX_ACTIVE_GROUPS = 5;

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final UserRepository userRepository;

	@Transactional
	public void create(Long userId, CreateGroupRequest request) {
		User user = userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));

		if (groupMemberRepository.countByUser_IdAndStatus(userId, GroupMemberStatus.ACTIVE)
				>= MAX_ACTIVE_GROUPS) {
			throw new ApiException(GroupErrorCode.GROUP_MAX_MEMBERSHIPS_REACHED);
		}

		Group group;
		try {
			group = groupRepository.saveAndFlush(Group.create(
					request.groupName(),
					request.roadAddress(),
					request.longitude(),
					request.latitude(),
					request.groupContent()
			));
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(GroupErrorCode.GROUP_NAME_ALREADY_USED, java.util.List.of(), exception);
		}
		groupMemberRepository.save(new GroupMember(group, user));
	}

	@Transactional
	public void join(Long userId, Long groupId) {
		User user = findActiveUser(userId);
		Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
				.orElseThrow(() -> new ApiException(GroupErrorCode.GROUP_NOT_FOUND));

		GroupMember member = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, userId).orElse(null);
		if (member != null) {
			if (member.isActive()) {
				throw new ApiException(GroupErrorCode.GROUP_ALREADY_JOINED);
			}
			checkGroupLimit(userId);
			member.join();
			return;
		}

		checkGroupLimit(userId);
		groupMemberRepository.save(new GroupMember(group, user));
	}

	@Transactional
	public void leave(Long userId, Long groupId) {
		Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
				.orElseThrow(() -> new ApiException(GroupErrorCode.GROUP_NOT_FOUND));
		GroupMember member = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
				.orElseThrow(() -> new ApiException(GroupErrorCode.GROUP_MEMBER_NOT_FOUND));

		if (!member.isActive()) {
			throw new ApiException(GroupErrorCode.GROUP_ALREADY_LEFT);
		}

		member.leave(LocalDateTime.now());
		if (groupMemberRepository.countByGroup_IdAndStatus(groupId, GroupMemberStatus.ACTIVE) == 0) {
			group.delete();
		}
	}

	private User findActiveUser(Long userId) {
		return userRepository.findActiveById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
	}

	private void checkGroupLimit(Long userId) {
		if (groupMemberRepository.countByUser_IdAndStatus(userId, GroupMemberStatus.ACTIVE)
				>= MAX_ACTIVE_GROUPS) {
			throw new ApiException(GroupErrorCode.GROUP_MAX_MEMBERSHIPS_REACHED);
		}
	}
}
