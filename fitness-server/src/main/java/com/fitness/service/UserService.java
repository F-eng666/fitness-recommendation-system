package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.ChangePasswordRequest;
import com.fitness.dto.LoginRequest;
import com.fitness.dto.RegisterRequest;
import com.fitness.dto.UserProfileRequest;

public interface UserService {
    Result login(LoginRequest request);
    Result register(RegisterRequest request);
    Result getUserProfile(Long userId);
    Result updateUserProfile(Long userId, UserProfileRequest request);
    Result changePassword(Long userId, ChangePasswordRequest request);
    Result getUserList(Integer page, Integer size, String keyword);
    Result updateUserStatus(Long userId, Integer status);
}