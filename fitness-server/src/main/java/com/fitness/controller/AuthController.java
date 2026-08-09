package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.LoginRequest;
import com.fitness.dto.RegisterRequest;
import com.fitness.entity.SysUser;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Resource
    private SysUserMapper sysUserMapper;

    // 内存存储验证码（生产环境应改用Redis）
    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private static class CodeEntry {
        String code;
        long expireAt;
        CodeEntry(String code, long expireAt) { this.code = code; this.expireAt = expireAt; }
    }

    @PostMapping("/login")
    public Result login(@RequestBody @Valid LoginRequest request) {
        log.info("User login: {}", request.getUsername());
        return userService.login(request);
    }

    @PostMapping("/register")
    public Result register(@RequestBody @Valid RegisterRequest request) {
        log.info("User register: {}", request.getUsername());
        return userService.register(request);
    }

    @GetMapping("/info")
    public Result getInfo() {
        Long userId = getCurrentUserId();
        log.info("Get user info: userId={}", userId);
        return userService.getUserProfile(userId);
    }

    @PostMapping("/logout")
    public Result logout() {
        log.info("User logout");
        return Result.success("退出成功");
    }

    /**
     * 发送验证码（用于找回密码）
     */
    @PostMapping("/send-code")
    public Result sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("请输入手机号");
        }
        // 检查手机号是否已注册
        SysUser user = sysUserMapper.selectByPhone(phone.trim());
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }
        // 生成6位验证码
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        // 5分钟有效
        codeStore.put(phone.trim(), new CodeEntry(code, System.currentTimeMillis() + 300_000));
        // 模拟发送验证码（生产环境对接短信平台）
        log.info("=== 验证码已发送至 {}: {} ===", phone, code);
        return Result.success("验证码已发送");
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result resetPassword(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        String newPassword = body.get("newPassword");

        if (phone == null || code == null || newPassword == null) {
            throw new BusinessException("参数不完整");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException("密码至少6位");
        }

        // 验证验证码
        CodeEntry entry = codeStore.get(phone.trim());
        if (entry == null) {
            throw new BusinessException("请先获取验证码");
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            codeStore.remove(phone.trim());
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!entry.code.equals(code.trim())) {
            throw new BusinessException("验证码错误");
        }

        // 重置密码
        SysUser user = sysUserMapper.selectByPhone(phone.trim());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        sysUserMapper.updateById(user);

        codeStore.remove(phone.trim());
        log.info("Password reset for user: {}", user.getUsername());
        return Result.success("密码重置成功");
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