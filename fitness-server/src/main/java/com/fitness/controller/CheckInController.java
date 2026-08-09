package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.CheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @Resource
    private SysUserMapper sysUserMapper;

    @PostMapping
    public Result checkIn(@RequestParam(required = false) Long courseId,
                          @RequestParam(required = false) Long venueId,
                          @RequestParam(required = false) String note) {
        Long userId = getCurrentUserId();
        log.info("Check in: userId={}, courseId={}, venueId={}, note={}", userId, courseId, venueId, note);
        return checkInService.checkIn(userId, courseId, venueId, note);
    }

    @GetMapping("/my")
    public Result my(@RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        log.info("Get my check-ins: userId={}, page={}, size={}", userId, page, size);
        return checkInService.getMyCheckIns(userId, page, size);
    }

    @GetMapping("/today/count")
    public Result todayCount() {
        Long userId = getCurrentUserId();
        log.info("Get today check-in count: userId={}", userId);
        return checkInService.getTodayCheckInCount(userId);
    }

    @GetMapping("/streak")
    public Result streak() {
        Long userId = getCurrentUserId();
        log.info("Get check-in streak: userId={}", userId);
        return checkInService.getCheckInStreak(userId);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id,
                         @RequestParam(required = false) Long venueId,
                         @RequestParam(required = false) String note) {
        Long userId = getCurrentUserId();
        log.info("Update check-in: id={}, userId={}, venueId={}, note={}", id, userId, venueId, note);
        return checkInService.updateCheckIn(id, userId, venueId, note);
    }

    @GetMapping("/rank")
    public Result rank(@RequestParam(required = false, defaultValue = "all") String type) {
        return checkInService.getCheckInRank(type);
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
