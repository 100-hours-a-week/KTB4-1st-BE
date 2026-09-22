package com.example.KTB_Agile_backend.group.repository;

import com.example.KTB_Agile_backend.group.dto.response.GroupSummary;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface GroupRepository extends JpaRepository<Group, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Group> findByIdAndDeletedAtIsNull(Long groupId);

	List<Group> findAllByIdInAndDeletedAtIsNull(Collection<Long> groupIds);

	@Query("""
			select new com.example.KTB_Agile_backend.group.dto.response.GroupSummary(
				g.id,
				g.groupName,
				g.roadAddress,
				g.groupContent,
				count(distinct member.id),
				count(distinct item.id),
				max(item.createdAt),
				case when count(distinct joinedMember.id) > 0 then true else false end
			)
			from Group g
			left join GroupMember member on member.group = g
				and member.status = :activeStatus
			left join GroupItem item on item.group = g
				and item.deletedAt is null
			left join GroupMember joinedMember on joinedMember.group = g
				and joinedMember.user.id = :userId
				and joinedMember.status = :activeStatus
			where g.deletedAt is null
				and g.groupName like concat('%', :keyword, '%')
			group by g.id, g.groupName, g.roadAddress, g.groupContent
			order by g.id desc
			""")
	List<GroupSummary> findSearchSummaries(
			@Param("userId") Long userId,
			@Param("activeStatus") GroupMemberStatus activeStatus,
			@Param("keyword") String keyword,
			Pageable pageable
	);

	@Query("""
			select new com.example.KTB_Agile_backend.group.dto.response.GroupSummary(
				g.id,
				g.groupName,
				g.roadAddress,
				g.groupContent,
				count(distinct member.id),
				count(distinct item.id),
				max(item.createdAt),
				case when count(distinct joinedMember.id) > 0 then true else false end
			)
			from Group g
			left join GroupMember member on member.group = g
				and member.status = :activeStatus
			left join GroupItem item on item.group = g
				and item.deletedAt is null
			left join GroupMember joinedMember on joinedMember.group = g
				and joinedMember.user.id = :userId
				and joinedMember.status = :activeStatus
			where g.deletedAt is null
				and g.groupName like concat('%', :keyword, '%')
				and g.id < :cursorId
			group by g.id, g.groupName, g.roadAddress, g.groupContent
			order by g.id desc
			""")
	List<GroupSummary> findSearchSummariesAfter(
			@Param("userId") Long userId,
			@Param("activeStatus") GroupMemberStatus activeStatus,
			@Param("keyword") String keyword,
			@Param("cursorId") Long cursorId,
			Pageable pageable
	);

	@Query("""
			select new com.example.KTB_Agile_backend.group.dto.response.GroupSummary(
				g.id,
				g.groupName,
				g.roadAddress,
				g.groupContent,
				count(distinct member.id),
				count(distinct item.id),
				max(item.createdAt),
				case when count(distinct joinedMember.id) > 0 then true else false end
			)
			from Group g
			left join GroupMember member on member.group = g
				and member.status = :activeStatus
			left join GroupItem item on item.group = g
				and item.deletedAt is null
			left join GroupMember joinedMember on joinedMember.group = g
				and joinedMember.user.id = :userId
				and joinedMember.status = :activeStatus
			where g.deletedAt is null
			group by g.id, g.groupName, g.roadAddress, g.groupContent
			order by count(distinct member.id) desc, g.id desc
			""")
	List<GroupSummary> findRecommendations(
			@Param("userId") Long userId,
			@Param("activeStatus") GroupMemberStatus activeStatus,
			Pageable pageable
	);

	@Query("""
			select new com.example.KTB_Agile_backend.group.dto.response.GroupSummary(
				g.id,
				g.groupName,
				g.roadAddress,
				g.groupContent,
				count(distinct member.id),
				count(distinct item.id),
				max(item.createdAt),
				case when count(distinct joinedMember.id) > 0 then true else false end
			)
			from Group g
			left join GroupMember member on member.group = g
				and member.status = :activeStatus
			left join GroupItem item on item.group = g
				and item.deletedAt is null
			left join GroupMember joinedMember on joinedMember.group = g
				and joinedMember.user.id = :userId
				and joinedMember.status = :activeStatus
			where g.deletedAt is null
			group by g.id, g.groupName, g.roadAddress, g.groupContent
			having count(distinct member.id) < :cursorMemberCount
				or (count(distinct member.id) = :cursorMemberCount and g.id < :cursorGroupId)
			order by count(distinct member.id) desc, g.id desc
			""")
	List<GroupSummary> findRecommendationsAfter(
			@Param("userId") Long userId,
			@Param("activeStatus") GroupMemberStatus activeStatus,
			@Param("cursorMemberCount") Long cursorMemberCount,
			@Param("cursorGroupId") Long cursorGroupId,
			Pageable pageable
	);
}
