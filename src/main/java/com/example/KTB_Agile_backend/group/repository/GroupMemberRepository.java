package com.example.KTB_Agile_backend.group.repository;

import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

	long countByUser_IdAndStatus(Long userId, GroupMemberStatus status);

	long countByGroup_IdAndStatus(Long groupId, GroupMemberStatus status);

	long countByGroup_IdInAndUser_IdAndStatus(
			Collection<Long> groupIds,
			Long userId,
			GroupMemberStatus status
	);

	Optional<GroupMember> findByGroup_IdAndUser_Id(Long groupId, Long userId);

	@Query("""
		select member.group.id as groupId, member.user.id as userId, member.status as membershipStatus
		from GroupMember member
		where member.group.id in :groupIds and member.user.id in :userIds
		""")
	List<GroupMemberStatusProjection> findStatusesByGroupIdsAndUserIds(
			@Param("groupIds") Collection<Long> groupIds,
			@Param("userIds") Collection<Long> userIds
	);

	interface GroupMemberStatusProjection {
		Long getGroupId();

		Long getUserId();

		GroupMemberStatus getMembershipStatus();
	}
}
