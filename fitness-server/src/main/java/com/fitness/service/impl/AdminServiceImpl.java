package com.fitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.Result;
import com.fitness.entity.*;
import com.fitness.mapper.*;
import com.fitness.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final SysUserMapper sysUserMapper;
    private final VenueMapper venueMapper;
    private final CourseMapper courseMapper;
    private final CourseOrderMapper courseOrderMapper;
    private final CheckInMapper checkInMapper;
    private final ArticleMapper articleMapper;
    private final CoachMapper coachMapper;

    @Override
    public Result getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        Long totalUsers = sysUserMapper.selectCount(null);
        stats.put("totalUsers", totalUsers);

        Long totalVenues = venueMapper.selectCount(null);
        stats.put("totalVenues", totalVenues);

        Long totalCoaches = coachMapper.selectCount(null);
        stats.put("totalCoaches", totalCoaches);

        // 场馆状态统计
        LambdaQueryWrapper<Venue> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.eq(Venue::getStatus, "APPROVED");
        stats.put("approvedVenues", venueMapper.selectCount(approvedWrapper));

        LambdaQueryWrapper<Venue> pendingVenueWrapper = new LambdaQueryWrapper<>();
        pendingVenueWrapper.eq(Venue::getStatus, "PENDING");
        stats.put("pendingVenues", venueMapper.selectCount(pendingVenueWrapper));

        LambdaQueryWrapper<Venue> rejectedWrapper = new LambdaQueryWrapper<>();
        rejectedWrapper.eq(Venue::getStatus, "REJECTED");
        stats.put("rejectedVenues", venueMapper.selectCount(rejectedWrapper));

        // 课程
        LambdaQueryWrapper<Course> upcomingCourseWrapper = new LambdaQueryWrapper<>();
        upcomingCourseWrapper.eq(Course::getStatus, "UPCOMING");
        stats.put("totalCourses", courseMapper.selectCount(upcomingCourseWrapper));

        // 用户统计
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime weekStart = todayStart.minusDays(7);
        LocalDateTime monthStart = todayStart.withDayOfMonth(1);

        LambdaQueryWrapper<SysUser> weekUserWrapper = new LambdaQueryWrapper<>();
        weekUserWrapper.ge(SysUser::getCreateTime, weekStart);
        stats.put("weekNewUsers", sysUserMapper.selectCount(weekUserWrapper));

        LambdaQueryWrapper<SysUser> monthUserWrapper = new LambdaQueryWrapper<>();
        monthUserWrapper.ge(SysUser::getCreateTime, monthStart);
        stats.put("monthNewUsers", sysUserMapper.selectCount(monthUserWrapper));

        // 活跃用户(本月有行为)
        stats.put("activeUsers", totalUsers);

        // 订单统计
        LambdaQueryWrapper<CourseOrder> todayOrderWrapper = new LambdaQueryWrapper<>();
        todayOrderWrapper.between(CourseOrder::getCreateTime, todayStart, todayEnd);
        stats.put("todayOrders", courseOrderMapper.selectCount(todayOrderWrapper));

        LambdaQueryWrapper<CourseOrder> weekOrderWrapper = new LambdaQueryWrapper<>();
        weekOrderWrapper.ge(CourseOrder::getCreateTime, weekStart);
        stats.put("weekOrders", courseOrderMapper.selectCount(weekOrderWrapper));

        // 营收统计（按实际课程价格计算）
        LambdaQueryWrapper<CourseOrder> allOrderWrapper = new LambdaQueryWrapper<>();
        List<CourseOrder> allOrders = courseOrderMapper.selectList(allOrderWrapper);

        double totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() != null && !"CANCELLED".equals(o.getStatus())
                        && !"REFUNDED".equals(o.getStatus()))
                .mapToDouble(o -> {
                    Course c = courseMapper.selectById(o.getCourseId());
                    return c != null && c.getPrice() != null ? c.getPrice().doubleValue() : 0;
                }).sum();
        stats.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);

        double todayRevenue = allOrders.stream()
                .filter(o -> o.getCreateTime() != null
                        && !o.getCreateTime().isBefore(todayStart)
                        && o.getCreateTime().isBefore(todayEnd)
                        && o.getStatus() != null && !"CANCELLED".equals(o.getStatus())
                        && !"REFUNDED".equals(o.getStatus()))
                .mapToDouble(o -> {
                    Course c = courseMapper.selectById(o.getCourseId());
                    return c != null && c.getPrice() != null ? c.getPrice().doubleValue() : 0;
                }).sum();
        stats.put("todayRevenue", Math.round(todayRevenue * 100.0) / 100.0);

        double monthRevenue = allOrders.stream()
                .filter(o -> o.getCreateTime() != null
                        && !o.getCreateTime().isBefore(monthStart)
                        && o.getStatus() != null && !"CANCELLED".equals(o.getStatus())
                        && !"REFUNDED".equals(o.getStatus()))
                .mapToDouble(o -> {
                    Course c = courseMapper.selectById(o.getCourseId());
                    return c != null && c.getPrice() != null ? c.getPrice().doubleValue() : 0;
                }).sum();
        stats.put("monthRevenue", Math.round(monthRevenue * 100.0) / 100.0);

        // 签到
        LambdaQueryWrapper<CheckIn> todayCheckInWrapper = new LambdaQueryWrapper<>();
        todayCheckInWrapper.apply("DATE(check_time) = CURDATE()");
        stats.put("todayCheckins", checkInMapper.selectCount(todayCheckInWrapper));

        // 社区
        LambdaQueryWrapper<Article> publishedArticleWrapper = new LambdaQueryWrapper<>();
        publishedArticleWrapper.eq(Article::getStatus, "PUBLISHED");
        stats.put("totalArticles", articleMapper.selectCount(publishedArticleWrapper));

        return Result.success(stats);
    }
}
