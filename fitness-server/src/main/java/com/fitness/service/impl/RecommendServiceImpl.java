package com.fitness.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fitness.common.Result;
import com.fitness.dto.BehaviorRecordRequest;
import com.fitness.entity.*;
import com.fitness.enums.TagTypeEnum;
import com.fitness.mapper.*;
import com.fitness.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.fitness.enums.TagTypeEnum.GOAL;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    private final UserTagMapper userTagMapper;
    private final CourseMapper courseMapper;
    private final CoachMapper coachMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final VenueMapper venueMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 健身目标 → 推荐课程类型映射
     */
    private static final Map<String, List<String>> GOAL_COURSE_TYPE_MAP = new HashMap<>();
    static {
        GOAL_COURSE_TYPE_MAP.put("LOSE_WEIGHT",   Arrays.asList("CARDIO","SPINNING","DANCE","BOXING","SWIMMING"));
        GOAL_COURSE_TYPE_MAP.put("BUILD_MUSCLE",  Arrays.asList("STRENGTH","BOXING","OTHER"));
        GOAL_COURSE_TYPE_MAP.put("SHAPE",         Arrays.asList("PILATES","YOGA","STRENGTH","DANCE"));
        GOAL_COURSE_TYPE_MAP.put("GAIN_WEIGHT",   Arrays.asList("STRENGTH","OTHER","YOGA"));
        GOAL_COURSE_TYPE_MAP.put("KEEP_HEALTHY",  Arrays.asList("YOGA","SWIMMING","PILATES","CARDIO","DANCE"));
    }

    /**
     * 健身目标 → 推荐教练专长映射
     */
    private static final Map<String, List<String>> GOAL_SPECIALTY_MAP = new HashMap<>();
    static {
        GOAL_SPECIALTY_MAP.put("LOSE_WEIGHT",  Arrays.asList("减脂","燃脂","有氧","HIIT","Tabata","动感单车","拳击","游泳","街舞","Zumba","CrossFit"));
        GOAL_SPECIALTY_MAP.put("BUILD_MUSCLE", Arrays.asList("增肌","力量","CrossFit","举重","拳击","散打","泰拳"));
        GOAL_SPECIALTY_MAP.put("SHAPE",        Arrays.asList("塑形","普拉提","瑜伽","体态","舞蹈","核心"));
        GOAL_SPECIALTY_MAP.put("GAIN_WEIGHT",  Arrays.asList("增肌","力量","举重","CrossFit","体能"));
        GOAL_SPECIALTY_MAP.put("KEEP_HEALTHY", Arrays.asList("瑜伽","游泳","普拉提","康复","体态","拉伸"));
    }

    @Override
    public Result getRecommendedCourses(Long userId, Integer limit) {
        // 确保用户的目标标签存在
        ensureGoalTags(userId);

        List<UserTag> userTags = userTagMapper.selectByUserId(userId);
        if (userTags == null || userTags.isEmpty()) {
            // 仍然没有标签（用户没有设置健身目标），按热度返回
            LambdaQueryWrapper<Course> topWrapper = new LambdaQueryWrapper<>();
            topWrapper.eq(Course::getStatus, "UPCOMING")
                    .orderByDesc(Course::getEnrolledCount)
                    .last("LIMIT " + limit);
            List<Course> topCourses = courseMapper.selectList(topWrapper);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Course course : topCourses) {
                Venue venue = venueMapper.selectById(course.getVenueId());
                Map<String, Object> item = new HashMap<>();
                item.put("course", course);
                item.put("score", 0.5);
                item.put("venueName", venue != null ? venue.getName() : null);
                result.add(item);
            }
            return Result.success(result);
        }

        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.eq(Course::getStatus, "UPCOMING");
        List<Course> allCourses = courseMapper.selectList(courseWrapper);

        List<Map<String, Object>> scoredCourses = new ArrayList<>();
        for (Course course : allCourses) {
            double score = 0.0;
            Set<String> courseTagSet = parseCourseTags(course);
            for (UserTag tag : userTags) {
                if (tag.getTagName() != null && containsTag(courseTagSet, tag.getTagName())) {
                    score += tag.getWeight().doubleValue() * 3.0;
                }
            }
            double popularity = (course.getEnrolledCount() * 1.0 / Math.max(course.getMaxStudents(), 1)) * 2.0;
            score += popularity;
            if (course.getCreateTime() != null
                    && course.getCreateTime().isAfter(LocalDateTime.now().minusDays(7))) {
                score += 1.0;
            }

            Venue venue = venueMapper.selectById(course.getVenueId());
            Map<String, Object> item = new HashMap<>();
            item.put("course", course);
            item.put("score", score);
            item.put("venueName", venue != null ? venue.getName() : null);
            scoredCourses.add(item);
        }

        scoredCourses.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        if (scoredCourses.size() > limit) {
            scoredCourses = scoredCourses.subList(0, limit);
        }

        return Result.success(scoredCourses);
    }

    @Override
    public Result getRecommendedCoaches(Long userId, Integer limit) {
        // 确保用户的目标标签存在
        ensureGoalTags(userId);

        List<UserTag> userTags = userTagMapper.selectByUserId(userId);

        LambdaQueryWrapper<Coach> coachWrapper = new LambdaQueryWrapper<>();
        coachWrapper.eq(Coach::getStatus, 1);
        List<Coach> allCoaches = coachMapper.selectList(coachWrapper);

        // 获取用户健身目标，用于加分
        SysUser user = sysUserMapper.selectById(userId);
        List<String> goalSpecialties = (user != null && user.getFitnessGoal() != null)
                ? GOAL_SPECIALTY_MAP.getOrDefault(user.getFitnessGoal(), Collections.emptyList())
                : Collections.emptyList();

        List<Map<String, Object>> scoredCoaches = new ArrayList<>();
        for (Coach coach : allCoaches) {
            double score = 0.0;

            // 目标匹配：教练专长跟用户目标匹配加分
            if (!goalSpecialties.isEmpty() && coach.getSpecialty() != null) {
                for (String gs : goalSpecialties) {
                    if (coach.getSpecialty().contains(gs)) {
                        score += 4.0;
                    }
                }
            }

            // 标签匹配
            if (userTags != null && !userTags.isEmpty()) {
                Set<String> coachSpecialties = parseCoachSpecialties(coach);
                for (UserTag tag : userTags) {
                    if (tag.getTagName() != null && containsTag(coachSpecialties, tag.getTagName())) {
                        score += tag.getWeight().doubleValue() * 3.0;
                    }
                }
            }
            double ratingScore = (coach.getRating() != null ? coach.getRating().doubleValue() : 0) * 2.0;
            score += ratingScore;

            Map<String, Object> item = new HashMap<>();
            item.put("coach", coach);
            item.put("score", score);
            scoredCoaches.add(item);
        }

        scoredCoaches.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        if (scoredCoaches.size() > limit) {
            scoredCoaches = scoredCoaches.subList(0, limit);
        }

        return Result.success(scoredCoaches);
    }

    /**
     * 确保用户健身目标对应的标签已写入 user_tag 表（类型 GOAL）
     */
    private void ensureGoalTags(Long userId) {
        // 检查是否已有 GOAL 标签，避免重复写入
        LambdaQueryWrapper<UserTag> goalWrapper = new LambdaQueryWrapper<>();
        goalWrapper.eq(UserTag::getUserId, userId)
                .eq(UserTag::getTagType, TagTypeEnum.GOAL.getCode());
        Long goalTagCount = userTagMapper.selectCount(goalWrapper);
        if (goalTagCount != null && goalTagCount > 0) {
            return; // 已有目标标签，跳过
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getFitnessGoal() == null || user.getFitnessGoal().isEmpty()) {
            return; // 用户未设置健身目标
        }

        List<String> targetTypes = GOAL_COURSE_TYPE_MAP.get(user.getFitnessGoal());
        if (targetTypes == null || targetTypes.isEmpty()) {
            return;
        }

        for (String type : targetTypes) {
            UserTag tag = new UserTag();
            tag.setUserId(userId);
            tag.setTagName(type);
            tag.setTagType(TagTypeEnum.GOAL.getCode());
            tag.setWeight(new BigDecimal("4.0"));
            tag.setCreateTime(LocalDateTime.now());
            tag.setUpdateTime(LocalDateTime.now());
            userTagMapper.insert(tag);
        }
        log.info("Created {} goal tags for user {} (goal={})", targetTypes.size(), userId, user.getFitnessGoal());
    }

    @Override
    public Result recordBehavior(Long userId, BehaviorRecordRequest request) {
        UserBehavior behavior = new UserBehavior();
        BeanUtil.copyProperties(request, behavior);
        behavior.setUserId(userId);
        behavior.setCreateTime(LocalDateTime.now());
        userBehaviorMapper.insert(behavior);
        return Result.success("行为记录成功");
    }

    @Override
    public Result updateUserTags(Long userId) {
        List<UserBehavior> behaviors = userBehaviorMapper.selectRecent30Days(userId);
        if (behaviors == null || behaviors.isEmpty()) {
            log.info("User {} has no recent behaviors, skipping tag update", userId);
            return Result.success("无最近行为数据");
        }

        Map<String, Double> tagFrequency = new HashMap<>();
        for (UserBehavior behavior : behaviors) {
            if (behavior.getTargetTags() == null || behavior.getTargetTags().isEmpty()) {
                continue;
            }
            double weight = getBehaviorWeight(behavior.getBehaviorType());
            String[] tags = behavior.getTargetTags().split(",");
            for (String tag : tags) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    tagFrequency.merge(trimmedTag, weight, Double::sum);
                }
            }
        }

        if (tagFrequency.isEmpty()) {
            return Result.success("无有效标签数据");
        }

        double maxFreq = tagFrequency.values().stream().max(Double::compare).orElse(1.0);

        LambdaQueryWrapper<UserTag> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserTag::getUserId, userId)
                .eq(UserTag::getTagType, TagTypeEnum.BEHAVIOR.getCode());
        userTagMapper.delete(deleteWrapper);

        for (Map.Entry<String, Double> entry : tagFrequency.entrySet()) {
            double normalizedWeight = (entry.getValue() / maxFreq) * 5.0;
            normalizedWeight = Math.min(normalizedWeight, 5.0);
            UserTag tag = new UserTag();
            tag.setUserId(userId);
            tag.setTagName(entry.getKey());
            tag.setTagType(TagTypeEnum.BEHAVIOR.getCode());
            tag.setWeight(BigDecimal.valueOf(normalizedWeight).setScale(2, RoundingMode.HALF_UP));
            tag.setCreateTime(LocalDateTime.now());
            tag.setUpdateTime(LocalDateTime.now());
            userTagMapper.insert(tag);
        }

        log.info("Updated behavior tags for user {}, tag count: {}", userId, tagFrequency.size());
        return Result.success("标签更新成功");
    }

    private Set<String> parseCourseTags(Course course) {
        Set<String> tags = new HashSet<>();
        if (course.getType() != null) {
            tags.add(course.getType());
        }
        if (course.getDifficulty() != null) {
            tags.add(course.getDifficulty());
        }
        if (course.getTags() != null && !course.getTags().isEmpty()) {
            String[] tagArray = course.getTags().split(",");
            for (String t : tagArray) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    tags.add(trimmed);
                }
            }
        }
        return tags;
    }

    private Set<String> parseCoachSpecialties(Coach coach) {
        Set<String> specialties = new HashSet<>();
        if (coach.getSpecialty() != null && !coach.getSpecialty().isEmpty()) {
            String[] specialtyArray = coach.getSpecialty().split(",");
            for (String s : specialtyArray) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    specialties.add(trimmed);
                }
            }
        }
        return specialties;
    }

    private boolean containsTag(Set<String> tagSet, String userTag) {
        for (String t : tagSet) {
            if (t.contains(userTag) || userTag.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private double getBehaviorWeight(String behaviorType) {
        if (behaviorType == null) {
            return 1.0;
        }
        switch (behaviorType) {
            case "BROWSE":
                return 1.0;
            case "FAVORITE":
                return 2.0;
            case "BOOK":
                return 3.0;
            case "COMMENT":
                return 2.0;
            case "LIKE":
                return 1.0;
            case "CHECK_IN":
                return 2.0;
            default:
                return 1.0;
        }
    }
}
