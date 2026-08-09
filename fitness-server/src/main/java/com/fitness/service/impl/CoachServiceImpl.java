package com.fitness.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.CoachRequest;
import com.fitness.entity.Coach;
import com.fitness.entity.Venue;
import com.fitness.mapper.CoachMapper;
import com.fitness.mapper.VenueMapper;
import com.fitness.service.CoachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoachServiceImpl implements CoachService {

    private final CoachMapper coachMapper;
    private final VenueMapper venueMapper;

    @Override
    public Result getCoachList(Integer page, Integer size, Long venueId, String keyword) {
        Page<Coach> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<>();
        if (venueId != null) {
            wrapper.eq(Coach::getVenueId, venueId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Coach::getName, keyword);
        }
        wrapper.orderByDesc(Coach::getRating);
        Page<Coach> pageResult = coachMapper.selectPage(pageParam, wrapper);
        return Result.success(PageResult.of(pageResult));
    }

    @Override
    public Result getCoachDetail(Long id) {
        Coach coach = coachMapper.selectById(id);
        if (coach == null) {
            throw new BusinessException("教练不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", coach.getId());
        result.put("name", coach.getName());
        result.put("avatar", coach.getAvatar());
        result.put("gender", coach.getGender());
        result.put("age", coach.getAge());
        result.put("experience", coach.getExperience());
        result.put("specialty", coach.getSpecialty());
        result.put("certInfo", coach.getCertInfo());
        result.put("introduction", coach.getIntroduction());
        result.put("venueId", coach.getVenueId());
        result.put("rating", coach.getRating());
        result.put("reviewCount", coach.getReviewCount());

        if (coach.getVenueId() != null) {
            Venue venue = venueMapper.selectById(coach.getVenueId());
            if (venue != null) {
                result.put("venueName", venue.getName());
                result.put("venueAddress", venue.getAddress());
            }
        }
        return Result.success(result);
    }

    @Override
    public Result createCoach(CoachRequest request) {
        Coach coach = new Coach();
        BeanUtil.copyProperties(request, coach);
        coach.setRating(new BigDecimal("0"));
        coach.setReviewCount(0);
        coach.setStatus(1);
        coach.setCreateTime(LocalDateTime.now());
        coach.setUpdateTime(LocalDateTime.now());
        coachMapper.insert(coach);
        return Result.success(coach);
    }

    @Override
    public Result updateCoach(Long id, CoachRequest request) {
        Coach coach = coachMapper.selectById(id);
        if (coach == null) {
            throw new BusinessException("教练不存在");
        }
        BeanUtil.copyProperties(request, coach, "id", "rating", "reviewCount", "status",
                "createTime", "updateTime");
        coach.setUpdateTime(LocalDateTime.now());
        coachMapper.updateById(coach);
        return Result.success(coach);
    }

    @Override
    public Result deleteCoach(Long id) {
        coachMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
