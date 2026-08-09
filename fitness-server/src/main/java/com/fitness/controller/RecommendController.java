package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.BehaviorRecordRequest;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/courses")
    public Result courses(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = getCurrentUserId();
        log.info("Get recommended courses: userId={}, limit={}", userId, limit);
        return recommendService.getRecommendedCourses(userId, limit);
    }

    @GetMapping("/coaches")
    public Result coaches(@RequestParam(defaultValue = "5") Integer limit) {
        Long userId = getCurrentUserId();
        log.info("Get recommended coaches: userId={}, limit={}", userId, limit);
        return recommendService.getRecommendedCoaches(userId, limit);
    }

    @PostMapping("/behavior")
    public Result recordBehavior(@RequestBody @Valid BehaviorRecordRequest request) {
        Long userId = getCurrentUserId();
        log.info("Record behavior: userId={}, behaviorType={}, targetType={}, targetId={}",
                userId, request.getBehaviorType(), request.getTargetType(), request.getTargetId());
        return recommendService.recordBehavior(userId, request);
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
