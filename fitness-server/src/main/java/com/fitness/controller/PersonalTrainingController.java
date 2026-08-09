package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.BookingRequest;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.PersonalTrainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/pt")
@RequiredArgsConstructor
public class PersonalTrainingController {

    private final PersonalTrainingService ptService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/slots")
    public Result getSlots(@RequestParam Long coachId,
                           @RequestParam Long venueId,
                           @RequestParam String date) {
        log.info("Get available slots: coachId={}, venueId={}, date={}", coachId, venueId, date);
        return ptService.getAvailableSlots(coachId, venueId, date);
    }

    @PostMapping("/book")
    public Result book(@RequestBody @Valid BookingRequest request) {
        Long userId = getCurrentUserId();
        log.info("Book personal training: userId={}, coachId={}", userId, request.getCoachId());
        return ptService.book(userId, request);
    }

    @GetMapping("/my")
    public Result myBookings() {
        Long userId = getCurrentUserId();
        return ptService.getMyBookings(userId);
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
                if (user != null) return user.getId();
            }
        }
        throw new BusinessException(401, "无法获取用户信息");
    }
}
