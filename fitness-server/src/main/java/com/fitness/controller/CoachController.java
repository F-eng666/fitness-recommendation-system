package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.CoachRequest;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.CoachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       @RequestParam(required = false) Long venueId,
                       @RequestParam(required = false) String keyword) {
        log.info("Get coach list: page={}, size={}, venueId={}, keyword={}", page, size, venueId, keyword);
        return coachService.getCoachList(page, size, venueId, keyword);
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        log.info("Get coach detail: id={}", id);
        return coachService.getCoachDetail(id);
    }

    @PostMapping("/create")
    public Result create(@RequestBody @Valid CoachRequest request) {
        log.info("Create coach: name={}", request.getName());
        return coachService.createCoach(request);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id,
                         @RequestBody @Valid CoachRequest request) {
        log.info("Update coach: id={}, name={}", id, request.getName());
        return coachService.updateCoach(id, request);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("Delete coach: id={}", id);
        return coachService.deleteCoach(id);
    }

    @SuppressWarnings("unused")
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
