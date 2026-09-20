package com.example.KTB_Agile_backend.group.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.group.dto.request.CreateGroupRequest;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import com.example.KTB_Agile_backend.group.repository.GroupMemberRepository;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

	@Test
	void createsGroupAndAddsCreatorAsActiveMember() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		GroupService service = new GroupService(groupRepository, groupMemberRepository, userRepository);
		User user = new User("사용자");
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(user));
		when(groupMemberRepository.countByUser_IdAndStatus(42L, GroupMemberStatus.ACTIVE)).thenReturn(4L);
		when(groupRepository.saveAndFlush(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.create(42L, new CreateGroupRequest(
				" 우리 그룹 ",
				" 주소 ",
				new BigDecimal("126.978000"),
				new BigDecimal("37.566500"),
				null
		));

		ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
		verify(groupRepository).saveAndFlush(groupCaptor.capture());
		assertThat(groupCaptor.getValue().getGroupName()).isEqualTo("우리 그룹");
		assertThat(groupCaptor.getValue().getGroupContent()).isEmpty();

		ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
		verify(groupMemberRepository).save(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getUser()).isSameAs(user);
		assertThat(memberCaptor.getValue().isActive()).isTrue();
	}

	@Test
	void rejectsCreationWhenUserAlreadyBelongsToFiveGroups() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		GroupService service = new GroupService(groupRepository, groupMemberRepository, userRepository);
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(new User("사용자")));
		when(groupMemberRepository.countByUser_IdAndStatus(42L, GroupMemberStatus.ACTIVE)).thenReturn(5L);

		ApiException exception = assertThrows(ApiException.class, () -> service.create(
				42L,
				new CreateGroupRequest("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "")
		));

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		verify(groupRepository, never()).saveAndFlush(any());
		verify(groupMemberRepository, never()).save(any());
	}

	@Test
	void rejectsDuplicateActiveGroupName() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		GroupService service = new GroupService(groupRepository, groupMemberRepository, userRepository);
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(new User("사용자")));
		when(groupMemberRepository.countByUser_IdAndStatus(42L, GroupMemberStatus.ACTIVE)).thenReturn(0L);
		when(groupRepository.saveAndFlush(any(Group.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate active group name"));

		ApiException exception = assertThrows(ApiException.class, () -> service.create(
				42L,
				new CreateGroupRequest("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "")
		));

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		verify(groupMemberRepository, never()).save(any());
	}

	@Test
	void joinsGroupWithNewMember() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		GroupService service = new GroupService(groupRepository, groupMemberRepository, userRepository);
		User user = new User("사용자");
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(user));
		when(groupRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(group));
		when(groupMemberRepository.findByGroup_IdAndUser_Id(7L, 42L)).thenReturn(Optional.empty());
		when(groupMemberRepository.countByUser_IdAndStatus(42L, GroupMemberStatus.ACTIVE)).thenReturn(4L);

		service.join(42L, 7L);

		ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
		verify(groupMemberRepository).save(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getGroup()).isSameAs(group);
		assertThat(memberCaptor.getValue().getUser()).isSameAs(user);
		assertThat(memberCaptor.getValue().isActive()).isTrue();
	}

	@Test
	void reactivatesLeftMemberWhenJoiningAgain() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		GroupService service = new GroupService(groupRepository, groupMemberRepository, userRepository);
		User user = new User("사용자");
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		GroupMember member = new GroupMember(group, user);
		member.leave(LocalDateTime.now());
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(user));
		when(groupRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(group));
		when(groupMemberRepository.findByGroup_IdAndUser_Id(7L, 42L)).thenReturn(Optional.of(member));
		when(groupMemberRepository.countByUser_IdAndStatus(42L, GroupMemberStatus.ACTIVE)).thenReturn(4L);

		service.join(42L, 7L);

		assertThat(member.isActive()).isTrue();
		assertThat(member.getLeftAt()).isNull();
		verify(groupMemberRepository, never()).save(any());
	}

	@Test
	void rejectsDuplicateJoin() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		GroupService service = new GroupService(groupRepository, groupMemberRepository, userRepository);
		User user = new User("사용자");
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(user));
		when(groupRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(group));
		when(groupMemberRepository.findByGroup_IdAndUser_Id(7L, 42L))
				.thenReturn(Optional.of(new GroupMember(group, user)));

		ApiException exception = assertThrows(ApiException.class, () -> service.join(42L, 7L));

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void leavesLastMemberAndSoftDeletesGroup() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		GroupService service = new GroupService(groupRepository, groupMemberRepository, userRepository);
		User user = new User("사용자");
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		GroupMember member = new GroupMember(group, user);
		when(groupRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(group));
		when(groupMemberRepository.findByGroup_IdAndUser_Id(7L, 42L)).thenReturn(Optional.of(member));
		when(groupMemberRepository.countByGroup_IdAndStatus(7L, GroupMemberStatus.ACTIVE)).thenReturn(0L);

		service.leave(42L, 7L);

		assertThat(member.isActive()).isFalse();
		assertThat(group.isDeleted()).isTrue();
	}

	@Test
	void rejectsDuplicateLeave() {
		GroupRepository groupRepository = mock(GroupRepository.class);
		GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		GroupService service = new GroupService(groupRepository, groupMemberRepository, userRepository);
		User user = new User("사용자");
		Group group = Group.create("그룹", "주소", BigDecimal.ZERO, BigDecimal.ZERO, "");
		GroupMember member = new GroupMember(group, user);
		member.leave(LocalDateTime.now());
		when(groupRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(group));
		when(groupMemberRepository.findByGroup_IdAndUser_Id(7L, 42L)).thenReturn(Optional.of(member));

		ApiException exception = assertThrows(ApiException.class, () -> service.leave(42L, 7L));

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
	}
}
