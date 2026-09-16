package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.repository.RefreshTokenRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountWithdrawalService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	public void withdraw(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("user not found"));

		LocalDateTime withdrawnAt = LocalDateTime.now();
		user.withdraw(withdrawnAt);
		refreshTokenRepository.revokeAllByUserId(userId, withdrawnAt);
	}
}
