package com.example.KTB_Agile_backend.user.service;

import com.example.KTB_Agile_backend.user.entity.User;

public record AccountResult(User user, boolean newUser) {
}
