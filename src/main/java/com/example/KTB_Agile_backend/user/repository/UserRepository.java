package com.example.KTB_Agile_backend.user.repository;

import com.example.KTB_Agile_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
