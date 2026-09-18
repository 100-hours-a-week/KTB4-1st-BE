package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.user.dto.request.UserPreferenceRequest;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.entity.UserPreference;
import com.example.KTB_Agile_backend.user.entity.UserPreferenceAnswer;
import com.example.KTB_Agile_backend.user.repository.UserPreferenceRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

	private final UserRepository userRepository;
	private final UserPreferenceRepository userPreferenceRepository;

	@Transactional
	public void create(Long userId, UserPreferenceRequest request) {
		User user = userRepository.findActiveById(userId)
				.orElseThrow(UserPreferenceService::unauthorized);
		if (userPreferenceRepository.existsByUser_Id(userId)) {
			throw alreadyExists();
		}

		List<UserPreferenceAnswer> answers = request.answers().stream()
				.map(answer -> new UserPreferenceAnswer(answer.question(), answer.answer()))
				.toList();
		try {
			userPreferenceRepository.saveAndFlush(new UserPreference(user, answers));
		} catch (DataIntegrityViolationException ignored) {
			throw alreadyExists();
		}
	}

	private static ApiException unauthorized() {
		return new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
	}

	private static ApiException alreadyExists() {
		return new ApiException(ErrorCode.USER_PREFERENCE_ALREADY_EXISTS, "사용자 설명은 회원가입 시 한 번만 설정할 수 있습니다.");
	}

}
