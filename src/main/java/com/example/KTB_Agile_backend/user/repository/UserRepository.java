package com.example.KTB_Agile_backend.user.repository;

import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByIdAndUserStatusAndDeletedAtIsNull(Long userId, UserStatus userStatus);

	default Optional<User> findActiveById(Long userId) {
		return findByIdAndUserStatusAndDeletedAtIsNull(userId, UserStatus.ACTIVE);
	}
}
