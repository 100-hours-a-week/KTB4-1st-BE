package com.example.KTB_Agile_backend.user.repository;

import com.example.KTB_Agile_backend.user.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

	boolean existsByUser_Id(Long userId);

	Optional<UserPreference> findByUser_Id(Long userId);
}
