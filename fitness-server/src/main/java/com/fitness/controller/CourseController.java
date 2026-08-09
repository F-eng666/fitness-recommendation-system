package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.CourseRequest;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String difficulty,
                       @RequestParam(required = false) Long venueId,
                       @RequestParam(required = false) Integer weekday,
                       @RequestParam(required = false) Integer published) {
        log.info("Get course list: page={}, size={}, keyword={}, type={}, difficulty={}, venueId={}, weekday={}, published={}",
                page, size, keyword, type, difficulty, venueId, weekday, published);
        // 默认只查已上架的课程，published=-1 时查全部（管理后台用）
        if (published == null) published = 1;
        if (published == -1) published = null;
        return courseService.getCourseList(page, size, keyword, type, difficulty, venueId, weekday, published);
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        Long userId = null;
        try { userId = getCurrentUserId(); } catch (Exception e) { /* 未登录也可查看 */ }
        log.info("Get course detail: id={}, userId={}", id, userId);
        return courseService.getCourseDetail(id, userId);
    }

    @PostMapping("/create")
    public Result create(@RequestBody @Valid CourseRequest request) {
        Long userId = getCurrentUserId();
        log.info("Create course: userId={}, name={}", userId, request.getName());
        return courseService.createCourse(request, userId);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id,
                         @RequestBody @Valid CourseRequest request) {
        log.info("Update course: id={}, name={}", id, request.getName());
        return courseService.updateCourse(id, request);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("Delete course: id={}", id);
        return courseService.deleteCourse(id);
    }

    @PutMapping("/{id}/recurrence")
    public Result setRecurrence(@PathVariable Long id, @RequestParam String recurrence) {
        log.info("Set course recurrence: id={}, recurrence={}", id, recurrence);
        return courseService.setRecurrence(id, recurrence);
    }

    @PutMapping("/{id}/publish")
    public Result togglePublished(@PathVariable Long id) {
        log.info("Toggle course published: id={}", id);
        return courseService.togglePublished(id);
    }

    @GetMapping("/{id}/dates")
    public Result dates(@PathVariable Long id) {
        log.info("Get course dates: id={}", id);
        return courseService.getCourseDates(id);
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
