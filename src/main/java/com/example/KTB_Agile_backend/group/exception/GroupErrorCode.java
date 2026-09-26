package com.example.KTB_Agile_backend.group.exception;

import com.example.KTB_Agile_backend.common.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

public enum GroupErrorCode implements ApiErrorCode {
	GROUP_MAX_MEMBERSHIPS_REACHED("GROUP_MAX_MEMBERSHIPS_REACHED", HttpStatus.CONFLICT, "가입할 수 있는 그룹은 최대 5개입니다."),
	GROUP_NAME_ALREADY_USED("GROUP_NAME_ALREADY_USED", HttpStatus.CONFLICT, "이미 사용 중인 그룹명입니다."),
	GROUP_NOT_FOUND("GROUP_NOT_FOUND", HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."),
	GROUP_ALREADY_JOINED("GROUP_ALREADY_JOINED", HttpStatus.CONFLICT, "이미 가입한 그룹입니다."),
	GROUP_MEMBER_NOT_FOUND("GROUP_MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "그룹 멤버를 찾을 수 없습니다."),
	GROUP_ALREADY_LEFT("GROUP_ALREADY_LEFT", HttpStatus.CONFLICT, "이미 탈퇴한 그룹입니다."),
	GROUP_MEMBERSHIP_REQUIRED("GROUP_MEMBERSHIP_REQUIRED", HttpStatus.FORBIDDEN, "그룹 멤버만 물품을 조회할 수 있습니다.");

	private final String value;
	private final HttpStatus status;
	private final String message;

	GroupErrorCode(String value, HttpStatus status, String message) {
		this.value = value;
		this.status = status;
		this.message = message;
	}

	@Override
	public String value() {
		return value;
	}

	@Override
	public HttpStatus status() {
		return status;
	}

	@Override
	public String message() {
		return message;
	}
}
