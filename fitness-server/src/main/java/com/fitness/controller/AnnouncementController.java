package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.AnnouncementRequest;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/active")
    public Result active() {
        log.info("Get active announcements");
        return announcementService.getActiveAnnouncements();
    }

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        log.info("Get announcement list: page={}, size={}", page, size);
        return announcementService.getAnnouncementList(page, size);
    }

    @PostMapping("/create")
    public Result create(@RequestBody @Valid AnnouncementRequest request) {
        Long userId = getCurrentUserId();
        log.info("Create announcement: userId={}, title={}", userId, request.getTitle());
        return announcementService.createAnnouncement(userId, request);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id,
                         @RequestBody @Valid AnnouncementRequest request) {
        log.info("Update announcement: id={}, title={}", id, request.getTitle());
        return announcementService.updateAnnouncement(id, request);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("Delete announcement: id={}", id);
        return announcementService.deleteAnnouncement(id);
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
