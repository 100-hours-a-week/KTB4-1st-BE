package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.user.dto.request.UserPreferenceRequest;
import com.example.KTB_Agile_backend.user.dto.response.UserPreferenceResponse;
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
	public UserPreferenceResponse create(Long userId, UserPreferenceRequest request) {
		User user = findActiveUser(userId);
		if (userPreferenceRepository.existsByUser_Id(userId)) {
			throw alreadyExists();
		}

		try {
			UserPreference preference = userPreferenceRepository.saveAndFlush(
					new UserPreference(user, answers(request))
			);
			return UserPreferenceResponse.from(preference);
		} catch (DataIntegrityViolationException ignored) {
			throw alreadyExists();
		}
	}

	@Transactional
	public void update(Long userId, UserPreferenceRequest request) {
		findActiveUser(userId);
		findPreference(userId).replaceAnswers(answers(request));
	}

	@Transactional(readOnly = true)
	public UserPreferenceResponse get(Long userId) {
		findActiveUser(userId);
		return UserPreferenceResponse.from(findPreference(userId));
	}

	private User findActiveUser(Long userId) {
		return userRepository.findActiveById(userId)
				.orElseThrow(UserPreferenceService::unauthorized);
	}

	private UserPreference findPreference(Long userId) {
		return userPreferenceRepository.findByUser_Id(userId)
				.orElseThrow(UserPreferenceService::notFound);
	}

	private static List<UserPreferenceAnswer> answers(UserPreferenceRequest request) {
		return request.answers().stream()
				.map(answer -> new UserPreferenceAnswer(answer.question(), answer.answer()))
				.toList();
	}

	private static ApiException unauthorized() {
		return new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
	}

	private static ApiException alreadyExists() {
		return new ApiException(ErrorCode.USER_PREFERENCE_ALREADY_EXISTS, "사용자 설명은 회원가입 시 한 번만 설정할 수 있습니다.");
	}

	private static ApiException notFound() {
		return new ApiException(ErrorCode.USER_PREFERENCE_NOT_FOUND, "사용자 취향이 설정되지 않았습니다.");
	}

}
