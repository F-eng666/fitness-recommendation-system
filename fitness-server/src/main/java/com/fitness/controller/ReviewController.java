package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.ReviewReplyRequest;
import com.fitness.dto.ReviewRequest;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Resource
    private SysUserMapper sysUserMapper;

    @PostMapping("/create")
    public Result create(@RequestBody @Valid ReviewRequest request) {
        Long userId = getCurrentUserId();
        log.info("Create review: userId={}, targetType={}, targetId={}, rating={}",
                userId, request.getTargetType(), request.getTargetId(), request.getRating());
        return reviewService.createReview(userId, request);
    }

    @GetMapping("/list")
    public Result list(@RequestParam String targetType,
                       @RequestParam Long targetId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        log.info("Get reviews: targetType={}, targetId={}, page={}, size={}",
                targetType, targetId, page, size);
        return reviewService.getTargetReviews(targetType, targetId, page, size);
    }

    @PutMapping("/{id}/reply")
    public Result reply(@PathVariable Long id,
                        @RequestBody @Valid ReviewReplyRequest request) {
        log.info("Reply review: reviewId={}", id);
        return reviewService.replyReview(id, request);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("Delete review: reviewId={}, userId={}", id, userId);
        return reviewService.deleteReview(id, userId);
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
