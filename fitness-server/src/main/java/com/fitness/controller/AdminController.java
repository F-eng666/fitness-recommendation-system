package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.AdminService;
import com.fitness.service.VenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final VenueService venueService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/dashboard")
    public Result dashboard() {
        log.info("Get admin dashboard stats");
        return adminService.getDashboardStats();
    }

    @GetMapping("/statistics")
    public Result statistics(@RequestParam String type,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate) {
        log.info("Get statistics: type={}, startDate={}, endDate={}", type, startDate, endDate);
        // AdminService only has getDashboardStats(), so for statistics we
        // delegate venue-related stats to VenueService if applicable.
        // For now, return dashboard stats as a general overview.
        return adminService.getDashboardStats();
    }

    @GetMapping("/venues/pending")
    public Result pendingVenues(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size) {
        log.info("Get pending venues: page={}, size={}", page, size);
        return venueService.getVenueList(page, size, null, "PENDING");
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
