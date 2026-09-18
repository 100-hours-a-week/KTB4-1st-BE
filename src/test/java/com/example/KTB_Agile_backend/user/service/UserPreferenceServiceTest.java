package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.user.dto.request.UserPreferenceRequest;
import com.example.KTB_Agile_backend.user.entity.User;
import com.example.KTB_Agile_backend.user.entity.UserPreference;
import com.example.KTB_Agile_backend.user.repository.UserPreferenceRepository;
import com.example.KTB_Agile_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

	@Test
	void savesAnyNumberOfAnswersForAnActiveUser() {
		UserRepository userRepository = mock(UserRepository.class);
		UserPreferenceRepository preferenceRepository = mock(UserPreferenceRepository.class);
		UserPreferenceService service = new UserPreferenceService(userRepository, preferenceRepository);
		User user = new User("nickname");
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(user));
		when(preferenceRepository.existsByUser_Id(42L)).thenReturn(false);
		when(preferenceRepository.saveAndFlush(any(UserPreference.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(42L, new UserPreferenceRequest(List.of(
				new UserPreferenceRequest.Answer("question-one", "answer-one"),
				new UserPreferenceRequest.Answer("question-two", "answer-two")
		)));

		ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
		verify(preferenceRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getAnswers()).hasSize(2);
		assertThat(response.answers()).hasSize(2);
	}

	@Test
	void rejectsPreferenceWhenItWasAlreadyCreated() {
		UserRepository userRepository = mock(UserRepository.class);
		UserPreferenceRepository preferenceRepository = mock(UserPreferenceRepository.class);
		UserPreferenceService service = new UserPreferenceService(userRepository, preferenceRepository);
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(new User("nickname")));
		when(preferenceRepository.existsByUser_Id(42L)).thenReturn(true);

		ApiException exception = assertThrows(ApiException.class, () -> service.create(
				42L,
				new UserPreferenceRequest(List.of(new UserPreferenceRequest.Answer("question", "answer")))
		));

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		verify(preferenceRepository, never()).saveAndFlush(any());
	}

	@Test
	void convertsConcurrentDuplicateCreationToConflict() {
		UserRepository userRepository = mock(UserRepository.class);
		UserPreferenceRepository preferenceRepository = mock(UserPreferenceRepository.class);
		UserPreferenceService service = new UserPreferenceService(userRepository, preferenceRepository);
		when(userRepository.findActiveById(42L)).thenReturn(Optional.of(new User("nickname")));
		when(preferenceRepository.existsByUser_Id(42L)).thenReturn(false);
		when(preferenceRepository.saveAndFlush(any(UserPreference.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate user preference"));

		ApiException exception = assertThrows(ApiException.class, () -> service.create(
				42L,
				new UserPreferenceRequest(List.of(new UserPreferenceRequest.Answer("question", "answer")))
		));

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
	}
}
