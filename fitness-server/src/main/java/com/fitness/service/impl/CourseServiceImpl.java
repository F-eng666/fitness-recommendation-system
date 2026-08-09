package com.fitness.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.CourseRequest;
import com.fitness.entity.Coach;
import com.fitness.entity.Course;
import com.fitness.entity.CourseOrder;
import com.fitness.entity.Venue;
import com.fitness.mapper.CoachMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.CourseOrderMapper;
import com.fitness.mapper.VenueMapper;
import com.fitness.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final VenueMapper venueMapper;
    private final CoachMapper coachMapper;
    private final CourseOrderMapper courseOrderMapper;

    @Override
    public Result getCourseList(Integer page, Integer size, String keyword, String type,
                                 String difficulty, Long venueId, Integer weekday, Integer published) {
        // 自动为每周重复课程生成未来实例
        generateRecurringCourseInstances();
        // 自动标记已过时间的课程为 FINISHED
        cleanExpiredCourses();

        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Course::getName, keyword);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Course::getType, type);
        }
        if (StringUtils.hasText(difficulty)) {
            wrapper.eq(Course::getDifficulty, difficulty);
        }
        if (venueId != null) {
            wrapper.eq(Course::getVenueId, venueId);
        }
        if (published != null) {
            wrapper.eq(Course::getPublished, published);
        }
        // 用户端查询（published 未传或为1）：只显示未结束的 UPCOMING 课程
        // 管理后台查询（published 为0或null即-1转换）：不做状态限制
        boolean isAdminQuery = published == null || (published != null && published != 1);
        if (!isAdminQuery) {
            wrapper.eq(Course::getStatus, "UPCOMING");
            wrapper.gt(Course::getEndTime, LocalDateTime.now());
        }
        wrapper.orderByDesc(Course::getPublished).orderByAsc(Course::getStartTime);
        List<Course> all = courseMapper.selectList(wrapper);
        List<Course> deduplicated = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Course c : all) {
            // 星期过滤
            if (weekday != null && c.getStartTime() != null) {
                int dow = c.getStartTime().getDayOfWeek().getValue(); // 1=Mon..7=Sun
                if (dow == 7) dow = 7; // keep Sunday as 7
                // User sends 1=Mon..7=Sun, Java DayOfWeek: 1=Mon..7=Sun
                if (dow != weekday) continue;
            }
            if ("WEEKLY".equals(c.getRecurrence())) {
                String key = c.getName() + "|" + c.getVenueId() + "|" + (c.getCoachId() != null ? c.getCoachId() : 0);
                if (seen.contains(key)) continue;
                seen.add(key);
            }
            deduplicated.add(c);
        }
        // 手动分页
        int from = (page - 1) * size;
        int to = Math.min(from + size, deduplicated.size());
        List<Course> paged = from < deduplicated.size() ? deduplicated.subList(from, to) : Collections.emptyList();

        // Enrich with venue names and coach names
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Course c : paged) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            item.put("description", c.getDescription());
            item.put("type", c.getType());
            item.put("difficulty", c.getDifficulty());
            item.put("maxStudents", c.getMaxStudents());
            item.put("enrolledCount", c.getEnrolledCount());
            item.put("coachId", c.getCoachId());
            item.put("venueId", c.getVenueId());
            item.put("price", c.getPrice());
            item.put("startTime", c.getStartTime() != null ? c.getStartTime().toString() : null);
            item.put("endTime", c.getEndTime() != null ? c.getEndTime().toString() : null);
            item.put("tags", c.getTags());
            item.put("status", c.getStatus());
            item.put("recurrence", c.getRecurrence());
            item.put("published", c.getPublished());
            item.put("createTime", c.getCreateTime() != null ? c.getCreateTime().toString() : null);

            // Venue name
            Venue venue = venueMapper.selectById(c.getVenueId());
            item.put("venueName", venue != null ? venue.getName() : "未知场馆");

            // Coach name
            if (c.getCoachId() != null) {
                Coach coach = coachMapper.selectById(c.getCoachId());
                item.put("coachName", coach != null ? coach.getName() : null);
            }

            // Rating: fetch from venue
            item.put("rating", venue != null ? venue.getRating() : null);

            enriched.add(item);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("total", deduplicated.size());
        map.put("page", page);
        map.put("size", size);
        map.put("records", enriched);
        return Result.success(map);
    }

    /**
     * 为标记为 WEEKLY 的课程自动生成未来 4 周的实例。
     * 不依赖 status 过滤模板，确保长时间无人访问后链条不会断裂。
     */
    private void generateRecurringCourseInstances() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusWeeks(4);

        // 查询所有 WEEKLY 课程（不限状态），按时间升序
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getRecurrence, "WEEKLY")
                .isNotNull(Course::getStartTime)
                .orderByAsc(Course::getStartTime);
        List<Course> allWeekly = courseMapper.selectList(wrapper);

        // 按 (name, venueId, coachId) 分组去重，优先取 UPCOMING 的，否则取 startTime 最新的
        Map<String, Course> distinctTemplates = new LinkedHashMap<>();
        for (Course c : allWeekly) {
            String key = c.getName() + "|" + c.getVenueId() + "|"
                    + (c.getCoachId() != null ? c.getCoachId() : 0);
            Course existing = distinctTemplates.get(key);
            if (existing == null) {
                distinctTemplates.put(key, c);
            } else if ("UPCOMING".equals(c.getStatus()) && !"UPCOMING".equals(existing.getStatus())) {
                distinctTemplates.put(key, c);
            } else if (!"UPCOMING".equals(c.getStatus()) && !"UPCOMING".equals(existing.getStatus())
                    && c.getStartTime().isAfter(existing.getStartTime())) {
                distinctTemplates.put(key, c);
            }
        }

        for (Course template : distinctTemplates.values()) {
            // 找到该模板已生成的最晚实例（不限状态）
            LambdaQueryWrapper<Course> instanceWrapper = new LambdaQueryWrapper<>();
            instanceWrapper.eq(Course::getName, template.getName())
                    .eq(Course::getVenueId, template.getVenueId())
                    .eq(Course::getCoachId, template.getCoachId())
                    .eq(Course::getRecurrence, "WEEKLY")
                    .orderByDesc(Course::getStartTime)
                    .last("LIMIT 1");
            Course lastInstance = courseMapper.selectOne(instanceWrapper);

            LocalDateTime nextStart;
            if (lastInstance != null && lastInstance.getStartTime() != null
                    && lastInstance.getStartTime().isAfter(template.getStartTime())) {
                nextStart = lastInstance.getStartTime().plusWeeks(1);
            } else {
                nextStart = template.getStartTime().plusWeeks(1);
            }

            // 如果计算出的 nextStart 已经过去（长时间无人访问链条断裂），从当前时间重新对齐
            if (nextStart.isBefore(now)) {
                int templateDayOfWeek = template.getStartTime().getDayOfWeek().getValue();
                LocalDateTime aligned = now.withHour(template.getStartTime().getHour())
                        .withMinute(template.getStartTime().getMinute())
                        .withSecond(0).withNano(0);
                int daysUntil = (templateDayOfWeek - aligned.getDayOfWeek().getValue() + 7) % 7;
                nextStart = aligned.plusDays(daysUntil);
                if (!nextStart.isAfter(now)) {
                    nextStart = nextStart.plusWeeks(1);
                }
            }

            // 生成直到 cutoff 的实例
            while (nextStart.isBefore(cutoff)) {
                LocalDateTime nextEnd = template.getEndTime() != null
                        ? nextStart.plusSeconds(java.time.Duration.between(
                                template.getStartTime(), template.getEndTime()).getSeconds())
                        : nextStart.plusHours(1);

                // 检查是否已存在
                LambdaQueryWrapper<Course> existWrapper = new LambdaQueryWrapper<>();
                existWrapper.eq(Course::getName, template.getName())
                        .eq(Course::getVenueId, template.getVenueId())
                        .eq(Course::getCoachId, template.getCoachId())
                        .eq(Course::getStartTime, nextStart);
                if (courseMapper.selectCount(existWrapper) == 0) {
                    Course instance = new Course();
                    instance.setName(template.getName());
                    instance.setDescription(template.getDescription());
                    instance.setType(template.getType());
                    instance.setDifficulty(template.getDifficulty());
                    instance.setMaxStudents(template.getMaxStudents());
                    instance.setEnrolledCount(0);
                    instance.setCoachId(template.getCoachId());
                    instance.setVenueId(template.getVenueId());
                    instance.setPrice(template.getPrice());
                    instance.setCoverImage(template.getCoverImage());
                    instance.setStartTime(nextStart);
                    instance.setEndTime(nextEnd);
                    instance.setTags(template.getTags());
                    instance.setStatus("UPCOMING");
                    instance.setRecurrence("WEEKLY");
                    instance.setPublished(1);
                    instance.setCreateTime(LocalDateTime.now());
                    instance.setUpdateTime(LocalDateTime.now());
                    courseMapper.insert(instance);
                    log.info("Generated recurring course: {} at {}", instance.getName(), nextStart);
                }

                nextStart = nextStart.plusWeeks(1);
            }
        }
    }

    @Override
    public Result getCourseDetail(Long id, Long userId) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        Venue venue = venueMapper.selectById(course.getVenueId());
        Coach coach = course.getCoachId() != null ? coachMapper.selectById(course.getCoachId()) : null;

        // 扁平化返回，前端直接作为 course 对象使用
        Map<String, Object> result = new HashMap<>();
        result.put("id", course.getId());
        result.put("name", course.getName());
        result.put("description", course.getDescription());
        result.put("type", course.getType());
        result.put("difficulty", course.getDifficulty());
        result.put("maxStudents", course.getMaxStudents());
        result.put("enrolledCount", course.getEnrolledCount());
        result.put("coachId", course.getCoachId());
        result.put("venueId", course.getVenueId());
        result.put("price", course.getPrice());
        result.put("coverImage", course.getCoverImage());
        result.put("startTime", course.getStartTime() != null ? course.getStartTime().toString() : null);
        result.put("endTime", course.getEndTime() != null ? course.getEndTime().toString() : null);
        result.put("tags", course.getTags());
        result.put("status", course.getStatus());
        result.put("recurrence", course.getRecurrence());
        result.put("createTime", course.getCreateTime() != null ? course.getCreateTime().toString() : null);

        // 关联场馆信息
        result.put("venueName", venue != null ? venue.getName() : null);
        result.put("venueAddress", venue != null ? venue.getAddress() : null);

        // 关联教练信息
        result.put("coachName", coach != null ? coach.getName() : null);
        result.put("coachAvatar", coach != null ? coach.getAvatar() : null);
        result.put("coachSpecialty", coach != null ? coach.getSpecialty() : null);
        result.put("coachExperience", coach != null ? coach.getExperience() : null);

        // 检查当前用户是否已预约此课程
        result.put("isBooked", false);
        if (userId != null) {
            LambdaQueryWrapper<CourseOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(CourseOrder::getUserId, userId)
                    .eq(CourseOrder::getCourseId, id)
                    .in(CourseOrder::getStatus, "PAID", "CONFIRMED", "PENDING");
            Long count = courseOrderMapper.selectCount(orderWrapper);
            result.put("isBooked", count > 0);
        }

        return Result.success(result);
    }

    @Override
    public Result createCourse(CourseRequest request, Long adminUserId) {
        Venue venue = venueMapper.selectById(request.getVenueId());
        if (venue == null) {
            throw new BusinessException("场馆不存在");
        }
        if (venue.getAdminUserId() != null && !venue.getAdminUserId().equals(adminUserId)) {
            throw new BusinessException("无权在此场馆创建课程");
        }
        if (request.getStartTime() != null && request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("开始时间不能是已过的时间");
        }
        if (request.getStartTime() != null && request.getEndTime() != null
                && !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        // 检查课程时间是否在场馆营业时间内
        if (StringUtils.hasText(venue.getBusinessHours())
                && request.getStartTime() != null && request.getEndTime() != null) {
            if (!isWithinBusinessHours(venue.getBusinessHours(),
                    request.getStartTime(), request.getEndTime())) {
                throw new BusinessException("课程时间不在场馆营业时间（"
                        + venue.getBusinessHours() + "）内");
            }
        }
        Course course = new Course();
        BeanUtil.copyProperties(request, course);
        course.setEnrolledCount(0);
        course.setStatus("UPCOMING");
        course.setPublished(1);
        course.setCreateTime(LocalDateTime.now());
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.insert(course);
        return Result.success(course);
    }

    @Override
    public Result updateCourse(Long id, CourseRequest request) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (request.getStartTime() != null && request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("开始时间不能是已过的时间");
        }
        if (request.getStartTime() != null && request.getEndTime() != null
                && !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        BeanUtil.copyProperties(request, course, "id", "enrolledCount", "status", "recurrence",
                "published", "createTime", "updateTime");
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
        return Result.success(course);
    }

    @Override
    public Result deleteCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        LambdaQueryWrapper<CourseOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(CourseOrder::getCourseId, id)
                .in(CourseOrder::getStatus, "PENDING", "CONFIRMED");
        Long activeOrderCount = courseOrderMapper.selectCount(orderWrapper);
        if (activeOrderCount > 0) {
            throw new BusinessException("该课程存在有效订单，无法删除");
        }
        courseMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @Override
    public Result setRecurrence(Long id, String recurrence) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        course.setRecurrence(recurrence);
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
        log.info("Course {} recurrence set to {}", id, recurrence);
        return Result.success(recurrence != null && !recurrence.isEmpty() ? "已设为每周重复" : "已取消每周重复");
    }

    @Override
    public Result togglePublished(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        int newPublished = (course.getPublished() != null && course.getPublished() == 1) ? 0 : 1;

        // 上架时检查课程时间是否已过期，若已过期需先重新设置日期
        if (newPublished == 1 && course.getEndTime() != null
                && course.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("该课程时间已过，请先修改课程日期后再重新上架");
        }

        // 上架时检查课程时间是否在场馆营业时间内
        if (newPublished == 1 && course.getVenueId() != null
                && course.getStartTime() != null && course.getEndTime() != null) {
            Venue venue = venueMapper.selectById(course.getVenueId());
            if (venue != null && StringUtils.hasText(venue.getBusinessHours())) {
                if (!isWithinBusinessHours(venue.getBusinessHours(),
                        course.getStartTime(), course.getEndTime())) {
                    throw new BusinessException("课程时间不在场馆营业时间（"
                            + venue.getBusinessHours() + "）内，无法上架");
                }
            }
        }

        // 上架时检查教练时间是否冲突
        if (newPublished == 1 && course.getCoachId() != null
                && course.getStartTime() != null && course.getEndTime() != null) {
            LambdaQueryWrapper<Course> conflictWrapper = new LambdaQueryWrapper<>();
            conflictWrapper.eq(Course::getCoachId, course.getCoachId())
                    .eq(Course::getPublished, 1)
                    .ne(Course::getId, id)
                    .lt(Course::getStartTime, course.getEndTime())
                    .gt(Course::getEndTime, course.getStartTime());
            Long conflictCount = courseMapper.selectCount(conflictWrapper);
            if (conflictCount > 0) {
                throw new BusinessException("该时间段教练已有其他课程，无法上架");
            }
        }

        course.setPublished(newPublished);
        // 上架时如果课程已过期且时间已更新为未来时间，重置状态为 UPCOMING
        if (newPublished == 1 && "FINISHED".equals(course.getStatus())
                && course.getEndTime() != null && course.getEndTime().isAfter(LocalDateTime.now())) {
            course.setStatus("UPCOMING");
        }
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
        log.info("Course {} published set to {}, status={}", id, newPublished, course.getStatus());
        return Result.success(newPublished == 1 ? "已上架" : "已下架");
    }

    @Override
    public Result getCourseDates(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) throw new BusinessException("课程不存在");

        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getName, course.getName())
                .eq(Course::getVenueId, course.getVenueId())
                .eq(Course::getStatus, "UPCOMING")
                .orderByAsc(Course::getStartTime);
        if (course.getCoachId() != null) {
            wrapper.eq(Course::getCoachId, course.getCoachId());
        }
        List<Course> instances = courseMapper.selectList(wrapper);

        List<Map<String, Object>> dates = new ArrayList<>();
        for (Course c : instances) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("startTime", c.getStartTime() != null ? c.getStartTime().toString() : null);
            item.put("endTime", c.getEndTime() != null ? c.getEndTime().toString() : null);
            item.put("enrolledCount", c.getEnrolledCount());
            item.put("maxStudents", c.getMaxStudents());
            dates.add(item);
        }
        return Result.success(dates);
    }

    /**
     * 检查课程时间是否在场馆营业时间内。
     * businessHours 格式如 "09:00-22:00"，支持跨天如 "22:00-02:00"
     */
    private boolean isWithinBusinessHours(String businessHours,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        try {
            String[] parts = businessHours.split("-");
            if (parts.length != 2) return true;
            int openMin = toMinutes(parts[0].trim());
            int closeMin = toMinutes(parts[1].trim());
            int startMin = startTime.getHour() * 60 + startTime.getMinute();
            int endMin = endTime.getHour() * 60 + endTime.getMinute();

            if (closeMin > openMin) {
                // 同一天：09:00-22:00，课程必须在 [open, close] 内
                return startMin >= openMin && endMin <= closeMin;
            } else {
                // 跨天：22:00-02:00，课程要么在 [open, 24:00] 要么在 [0, close]
                boolean startsInRange = startMin >= openMin || startMin < closeMin;
                boolean endsInRange = endMin <= closeMin || endMin > openMin;
                return startsInRange && endsInRange && startMin <= endMin;
            }
        } catch (Exception e) {
            return true;
        }
    }

    private int toMinutes(String time) {
        String[] p = time.split(":");
        return Integer.parseInt(p[0]) * 60 + (p.length > 1 ? Integer.parseInt(p[1]) : 0);
    }

    private void cleanExpiredCourses() {
        // 过期的 UPCOMING 课程标记为 FINISHED
        LambdaUpdateWrapper<Course> statusWrapper = new LambdaUpdateWrapper<>();
        statusWrapper.set(Course::getStatus, "FINISHED")
                .eq(Course::getStatus, "UPCOMING")
                .lt(Course::getEndTime, LocalDateTime.now());
        courseMapper.update(null, statusWrapper);

        // 过了结束时间的课程自动下架
        LambdaUpdateWrapper<Course> publishWrapper = new LambdaUpdateWrapper<>();
        publishWrapper.set(Course::getPublished, 0)
                .eq(Course::getPublished, 1)
                .lt(Course::getEndTime, LocalDateTime.now());
        courseMapper.update(null, publishWrapper);
    }
}
