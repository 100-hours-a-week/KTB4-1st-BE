package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.common.exception.ErrorCode;
import com.example.KTB_Agile_backend.user.dto.UserPreferenceQuestion;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

	private final UserRepository userRepository;
	private final UserPreferenceRepository userPreferenceRepository;

	@Transactional
	public UserPreferenceResponse create(Long userId, UserPreferenceRequest request) {
		List<UserPreferenceAnswer> preferenceAnswers = answers(request);
		User user = findActiveUser(userId);
		if (userPreferenceRepository.existsByUser_Id(userId)) {
			throw alreadyExists();
		}

		try {
			UserPreference preference = userPreferenceRepository.saveAndFlush(
					new UserPreference(user, preferenceAnswers)
			);
			return UserPreferenceResponse.from(preference);
		} catch (DataIntegrityViolationException ignored) {
			throw alreadyExists();
		}
	}

	@Transactional
	public UserPreferenceResponse update(Long userId, UserPreferenceRequest request) {
		List<UserPreferenceAnswer> preferenceAnswers = answers(request);
		findActiveUser(userId);
		UserPreference preference = findPreference(userId);
		preference.replaceAnswers(preferenceAnswers);
		return UserPreferenceResponse.from(preference);
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
		if (!hasAllQuestions(request.answers())) {
			throw invalidQuestions();
		}

		return request.answers().stream()
				.map(answer -> new UserPreferenceAnswer(answer.question().name(), answer.answer().name()))
				.toList();
	}

	private static boolean hasAllQuestions(List<UserPreferenceRequest.Answer> answers) {
		int requiredQuestionCount = UserPreferenceQuestion.values().length;
		return answers != null
				&& answers.size() == requiredQuestionCount
				&& answers.stream().allMatch(answer -> answer != null && answer.question() != null)
				&& answers.stream()
					.map(UserPreferenceRequest.Answer::question)
					.filter(Objects::nonNull)
					.distinct()
					.count() == requiredQuestionCount;
	}

	private static ApiException invalidQuestions() {
		return new ApiException(ErrorCode.USER_PREFERENCE_INVALID);
	}

	private static ApiException unauthorized() {
		return new ApiException(ErrorCode.AUTHENTICATION_REQUIRED);
	}

	private static ApiException alreadyExists() {
		return new ApiException(ErrorCode.USER_PREFERENCE_ALREADY_EXISTS);
	}

	private static ApiException notFound() {
		return new ApiException(ErrorCode.USER_PREFERENCE_NOT_FOUND);
	}

}
