package com.example.KTB_Agile_backend.group.repository;

import com.example.KTB_Agile_backend.group.entity.GroupMember;
import com.example.KTB_Agile_backend.group.entity.GroupMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

	long countByUser_IdAndStatus(Long userId, GroupMemberStatus status);

	long countByGroup_IdAndStatus(Long groupId, GroupMemberStatus status);

	Optional<GroupMember> findByGroup_IdAndUser_Id(Long groupId, Long userId);
}
