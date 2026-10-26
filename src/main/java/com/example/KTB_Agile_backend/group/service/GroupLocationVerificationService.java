package com.example.KTB_Agile_backend.group.service;

import com.example.KTB_Agile_backend.common.exception.ApiException;
import com.example.KTB_Agile_backend.group.dto.request.GroupLocationVerificationRequest;
import com.example.KTB_Agile_backend.group.entity.Group;
import com.example.KTB_Agile_backend.group.exception.GroupErrorCode;
import com.example.KTB_Agile_backend.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class GroupLocationVerificationService {

	private static final double EARTH_RADIUS_METERS = 6_371_000.0;
	private static final double VERIFICATION_RADIUS_METERS = 500.0;
	private static final BigDecimal MAX_ACCURACY_METERS = BigDecimal.valueOf(200);
	private static final Duration VERIFICATION_TTL = Duration.ofMinutes(5);
	private static final String KEY_PREFIX = "group:location-verification:";

	private final GroupRepository groupRepository;
	private final StringRedisTemplate redisTemplate;

	public void verify(
			Long userId,
			Long groupId,
			GroupLocationVerificationRequest request
	) {
		Group group = groupRepository.findActiveForLocationVerification(groupId)
				.orElseThrow(() -> new ApiException(GroupErrorCode.GROUP_NOT_FOUND));

		if (request.accuracy().compareTo(MAX_ACCURACY_METERS) > 0
				|| distanceMeters(group, request.latitude(), request.longitude()) > VERIFICATION_RADIUS_METERS) {
			throw new ApiException(GroupErrorCode.GROUP_LOCATION_VERIFICATION_FAILED);
		}

		redisTemplate.opsForValue().set(verificationKey(userId, groupId), "verified", VERIFICATION_TTL);
	}

	public void consume(Long userId, Long groupId) {
		if (!Boolean.TRUE.equals(redisTemplate.delete(verificationKey(userId, groupId)))) {
			throw new ApiException(GroupErrorCode.GROUP_LOCATION_VERIFICATION_REQUIRED);
		}
	}

	private static double distanceMeters(Group group, BigDecimal latitude, BigDecimal longitude) {
		double groupLatitude = Math.toRadians(group.getLatitude().doubleValue());
		double requestLatitude = Math.toRadians(latitude.doubleValue());
		double latitudeDifference = requestLatitude - groupLatitude;
		double longitudeDifference = Math.toRadians(longitude.doubleValue() - group.getLongitude().doubleValue());
		double halfChord = Math.pow(Math.sin(latitudeDifference / 2), 2)
				+ Math.cos(groupLatitude) * Math.cos(requestLatitude)
				* Math.pow(Math.sin(longitudeDifference / 2), 2);
		return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(Math.min(1.0, halfChord)));
	}

	private static String verificationKey(Long userId, Long groupId) {
		return KEY_PREFIX + groupId + ":" + userId;
	}
}
