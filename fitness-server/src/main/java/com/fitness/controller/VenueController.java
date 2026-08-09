package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.VenueRequest;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.VenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/venue")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "APPROVED") String status) {
        log.info("Get venue list: page={}, size={}, keyword={}, status={}", page, size, keyword, status);
        return venueService.getVenueList(page, size, keyword, status);
    }

    @GetMapping("/my")
    public Result my() {
        Long userId = getCurrentUserId();
        log.info("Get my venues: userId={}", userId);
        return venueService.getMyVenues(userId);
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        log.info("Get venue detail: id={}", id);
        return venueService.getVenueDetail(id);
    }

    @PostMapping("/create")
    public Result create(@RequestBody @Valid VenueRequest request) {
        Long userId = getCurrentUserId();
        log.info("Create venue: userId={}, name={}", userId, request.getName());
        return venueService.createVenue(request, userId);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id,
                         @RequestBody @Valid VenueRequest request) {
        log.info("Update venue: id={}, name={}", id, request.getName());
        return venueService.updateVenue(id, request);
    }

    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Long id,
                               @RequestParam String status) {
        log.info("Update venue status: id={}, status={}", id, status);
        return venueService.updateVenueStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("Delete venue: id={}", id);
        return venueService.deleteVenue(id);
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
