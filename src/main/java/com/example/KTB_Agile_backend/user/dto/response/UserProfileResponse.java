package com.example.KTB_Agile_backend.user.dto.response;

import com.example.KTB_Agile_backend.user.entity.User;

public record UserProfileResponse(String nickname, String profileImageUrl) {

	public static UserProfileResponse from(User user) {
		return new UserProfileResponse(user.getNickname(), user.getProfileImageUrl());
	}
}
