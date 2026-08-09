package com.fitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.entity.*;
import com.fitness.mapper.*;
import com.fitness.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ArticleMapper articleMapper;
    private final CourseMapper courseMapper;
    private final VenueMapper venueMapper;
    private final CoachMapper coachMapper;

    @Override
    public Result toggleFavorite(Long userId, String targetType, Long targetId) {
        Favorite existing = favoriteMapper.selectByUserAndTarget(userId, targetType, targetId);
        if (existing != null) {
            favoriteMapper.deleteByUserAndTarget(userId, targetType, targetId);
            return Result.success(Map.of("isFavorited", false));
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTargetType(targetType);
        favorite.setTargetId(targetId);
        favorite.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        return Result.success(Map.of("isFavorited", true));
    }

    @Override
    public Result getMyFavorites(Long userId, Integer page, Integer size, String targetType) {
        Page<Favorite> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(Favorite::getTargetType, targetType);
        }
        wrapper.orderByDesc(Favorite::getCreateTime);
        Page<Favorite> pageResult = favoriteMapper.selectPage(pageParam, wrapper);

        // 关联查询目标对象的实际名称和描述
        List<Map<String, Object>> enrichedList = new ArrayList<>();
        for (Favorite fav : pageResult.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", fav.getId());
            item.put("userId", fav.getUserId());
            item.put("targetType", fav.getTargetType());
            item.put("targetId", fav.getTargetId());
            item.put("createTime", fav.getCreateTime());
            enrichTargetInfo(fav.getTargetType(), fav.getTargetId(), item);
            enrichedList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("records", enrichedList);
        return Result.success(result);
    }

    private void enrichTargetInfo(String targetType, Long targetId, Map<String, Object> item) {
        try {
            switch (targetType) {
                case "ARTICLE":
                    Article article = articleMapper.selectById(targetId);
                    if (article != null) {
                        item.put("targetName", article.getTitle());
                        item.put("description", article.getSummary());
                    }
                    break;
                case "COURSE":
                    Course course = courseMapper.selectById(targetId);
                    if (course != null) {
                        item.put("targetName", course.getName());
                        item.put("description", course.getType());
                        item.put("price", course.getPrice());
                    }
                    break;
                case "VENUE":
                    Venue venue = venueMapper.selectById(targetId);
                    if (venue != null) {
                        item.put("targetName", venue.getName());
                        item.put("description", venue.getAddress());
                    }
                    break;
                case "COACH":
                    Coach coach = coachMapper.selectById(targetId);
                    if (coach != null) {
                        item.put("targetName", coach.getName());
                        item.put("description", coach.getSpecialty());
                    }
                    break;
                default:
                    item.put("targetName", targetType + " #" + targetId);
                    item.put("description", "");
            }
        } catch (Exception e) {
            item.put("targetName", targetType + " #" + targetId);
            item.put("description", "");
        }
    }

    @Override
    public Result isFavorited(Long userId, String targetType, Long targetId) {
        Favorite existing = favoriteMapper.selectByUserAndTarget(userId, targetType, targetId);
        return Result.success(existing != null);
    }
}
