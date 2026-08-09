package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Resource
    private SysUserMapper sysUserMapper;

    @PostMapping("/toggle")
    public Result toggle(@RequestParam String targetType,
                         @RequestParam Long targetId) {
        Long userId = getCurrentUserId();
        log.info("Toggle favorite: userId={}, targetType={}, targetId={}", userId, targetType, targetId);
        return favoriteService.toggleFavorite(userId, targetType, targetId);
    }

    @GetMapping("/my")
    public Result my(@RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "10") Integer size,
                     @RequestParam(required = false) String targetType) {
        Long userId = getCurrentUserId();
        log.info("Get my favorites: userId={}, page={}, size={}, targetType={}", userId, page, size, targetType);
        return favoriteService.getMyFavorites(userId, page, size, targetType);
    }

    @GetMapping("/check")
    public Result check(@RequestParam String targetType,
                        @RequestParam Long targetId) {
        Long userId = getCurrentUserId();
        log.info("Check favorite: userId={}, targetType={}, targetId={}", userId, targetType, targetId);
        return favoriteService.isFavorited(userId, targetType, targetId);
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
