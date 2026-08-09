package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.ChangePasswordRequest;
import com.fitness.dto.UserProfileRequest;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/profile")
    public Result getProfile() {
        Long userId = getCurrentUserId();
        log.info("Get user profile: userId={}", userId);
        return userService.getUserProfile(userId);
    }

    @PutMapping("/profile")
    public Result updateProfile(@RequestBody @Valid UserProfileRequest request) {
        Long userId = getCurrentUserId();
        log.info("Update user profile: userId={}", userId);
        return userService.updateUserProfile(userId, request);
    }

    @PutMapping("/password")
    public Result changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        Long userId = getCurrentUserId();
        log.info("Change password: userId={}", userId);
        return userService.changePassword(userId, request);
    }

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       @RequestParam(required = false) String keyword) {
        log.info("Get user list: page={}, size={}, keyword={}", page, size, keyword);
        return userService.getUserList(page, size, keyword);
    }

    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Long id,
                               @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        log.info("Update user status: userId={}, status={}", id, status);
        return userService.updateUserStatus(id, status);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(401, "请先登录");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String) {
            String str = (String) principal;
            try {
                return Long.valueOf(str);
            } catch (NumberFormatException e) {
                var user = sysUserMapper.selectByUsername(str);
                if (user != null) {
                    return user.getId();
                }
            }
        }
        throw new BusinessException(401, "无法获取用户信息");
    }
}
