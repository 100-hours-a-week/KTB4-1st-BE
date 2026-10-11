package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.user.dto.response.UserProfileResponse;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileQueryService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public UserProfileResponse get(Long userId) {
		return userRepository.findActiveById(userId)
				.map(UserProfileResponse::from)
				.orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
	}
}
