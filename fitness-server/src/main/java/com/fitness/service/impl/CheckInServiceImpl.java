package com.fitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.entity.CheckIn;
import com.fitness.entity.Course;
import com.fitness.entity.SysUser;
import com.fitness.entity.Venue;
import com.fitness.mapper.CheckInMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.SysUserMapper;
import com.fitness.mapper.VenueMapper;
import com.fitness.service.CheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInServiceImpl implements CheckInService {

    private final CheckInMapper checkInMapper;
    private final SysUserMapper sysUserMapper;
    private final VenueMapper venueMapper;
    private final CourseMapper courseMapper;

    @Override
    public Result checkIn(Long userId, Long courseId, Long venueId, String note) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CheckIn> todayWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        todayWrapper.eq(CheckIn::getUserId, userId)
                .apply("DATE(check_time) = CURDATE()");
        Long todayCount = checkInMapper.selectCount(todayWrapper);
        if (todayCount > 0) {
            return Result.error("今日已打卡");
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setUserId(userId);
        checkIn.setCourseId(courseId);
        checkIn.setVenueId(venueId);
        checkIn.setNote(note);
        checkIn.setCheckTime(java.time.LocalDateTime.now());
        checkIn.setStatus(1);
        checkInMapper.insert(checkIn);
        return Result.success("打卡成功");
    }

    @Override
    public Result updateCheckIn(Long id, Long userId, Long venueId, String note) {
        CheckIn checkIn = checkInMapper.selectById(id);
        if (checkIn == null || !checkIn.getUserId().equals(userId)) {
            throw new BusinessException("打卡记录不存在");
        }
        // Only allow editing today's check-in
        if (!checkIn.getCheckTime().toLocalDate().equals(LocalDate.now())) {
            throw new BusinessException("只能修改当天的打卡记录");
        }
        if (venueId != null) {
            checkIn.setVenueId(venueId);
        }
        if (note != null) {
            checkIn.setNote(note);
        }
        checkInMapper.updateById(checkIn);
        return Result.success("打卡记录已更新");
    }

    @Override
    public Result getMyCheckIns(Long userId, Integer page, Integer size) {
        Page<CheckIn> pageParam = new Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CheckIn> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getUserId, userId)
                .orderByDesc(CheckIn::getCheckTime);
        Page<CheckIn> pageResult = checkInMapper.selectPage(pageParam, wrapper);

        // Enrich with venue and course names
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (CheckIn c : pageResult.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("userId", c.getUserId());
            item.put("courseId", c.getCourseId());
            item.put("venueId", c.getVenueId());
            item.put("note", c.getNote());
            item.put("checkTime", c.getCheckTime() != null ? c.getCheckTime().toString() : null);
            item.put("status", c.getStatus());

            // Venue name
            if (c.getVenueId() != null) {
                Venue venue = venueMapper.selectById(c.getVenueId());
                item.put("venueName", venue != null ? venue.getName() : null);
            }

            // Course info
            if (c.getCourseId() != null) {
                Course course = courseMapper.selectById(c.getCourseId());
                if (course != null) {
                    item.put("courseName", course.getName());
                    item.put("courseTime", (course.getStartTime() != null ? course.getStartTime().toString() : "")
                            + " ~ " + (course.getEndTime() != null ? course.getEndTime().toString() : ""));
                }
            }

            enriched.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", enriched);
        result.put("total", pageResult.getTotal());
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return Result.success(result);
    }

    @Override
    public Result getTodayCheckInCount(Long userId) {
        int count = checkInMapper.countTodayCheckIn(userId);
        return Result.success(Map.of("count", count));
    }

    @Override
    public Result getCheckInStreak(Long userId) {
        List<CheckIn> checkIns = checkInMapper.selectByUserId(userId);
        int streak = 0;
        LocalDate today = LocalDate.now();
        if (!checkIns.isEmpty()) {
            streak = 1;
            LocalDate lastDate = checkIns.get(0).getCheckTime().toLocalDate();
            if (today.equals(lastDate) || today.minusDays(1).equals(lastDate)) {
                for (int i = 1; i < checkIns.size(); i++) {
                    LocalDate currentDate = checkIns.get(i).getCheckTime().toLocalDate();
                    if (lastDate.minusDays(1).equals(currentDate)) {
                        streak++;
                        lastDate = currentDate;
                    } else {
                        break;
                    }
                }
            }
        }
        return Result.success(Map.of("streak", streak, "totalDays", checkIns.size()));
    }

    @Override
    public Result getCheckInRank(String type) {
        List<Map<String, Object>> rankList = checkInMapper.selectMaps(
                new QueryWrapper<CheckIn>()
                        .select("user_id, COUNT(*) as total")
                        .groupBy("user_id")
                        .orderByDesc("total")
                        .last("LIMIT 10"));

        // 补充用户头像和昵称
        List<Map<String, Object>> enriched = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : rankList) {
            Long userId = (Long) row.get("user_id");
            SysUser user = sysUserMapper.selectById(userId);
            Map<String, Object> item = new HashMap<>();
            item.put("userId", userId);
            item.put("count", row.get("total"));
            item.put("nickname", user != null && user.getNickname() != null ? user.getNickname() : (user != null ? user.getUsername() : "未知"));
            item.put("avatar", user != null ? user.getAvatar() : null);
            item.put("rank", rank++);
            enriched.add(item);
        }
        return Result.success(enriched);
    }
}