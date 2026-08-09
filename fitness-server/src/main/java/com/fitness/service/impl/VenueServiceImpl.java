package com.fitness.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.VenueRequest;
import com.fitness.entity.Venue;
import com.fitness.mapper.ReviewMapper;
import com.fitness.mapper.VenueMapper;
import com.fitness.service.VenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueServiceImpl implements VenueService {

    private final VenueMapper venueMapper;
    private final ReviewMapper reviewMapper;

    @Override
    public Result getVenueList(Integer page, Integer size, String keyword, String status) {
        Page<Venue> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Venue::getName, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Venue::getStatus, status);
        }
        wrapper.orderByDesc(Venue::getRating);
        Page<Venue> pageResult = venueMapper.selectPage(pageParam, wrapper);
        return Result.success(PageResult.of(pageResult));
    }

    @Override
    public Result getMyVenues(Long adminUserId) {
        List<Venue> venues = venueMapper.selectByAdminUserId(adminUserId);
        return Result.success(venues);
    }

    @Override
    public Result getVenueDetail(Long id) {
        Venue venue = venueMapper.selectById(id);
        if (venue == null) {
            throw new BusinessException("场馆不存在");
        }
        Double avgRating = null;
        try {
            avgRating = reviewMapper.selectAvgRating("VENUE", id);
        } catch (Exception e) {
            log.warn("Failed to query review avg rating for venue {}: {}", id, e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("venue", venue);
        result.put("avgRating", avgRating);
        return Result.success(result);
    }

    @Override
    public Result createVenue(VenueRequest request, Long adminUserId) {
        Venue venue = new Venue();
        BeanUtil.copyProperties(request, venue);
        venue.setAdminUserId(adminUserId);
        venue.setStatus("PENDING");
        venue.setRating(new java.math.BigDecimal("0"));
        venue.setReviewCount(0);
        venue.setCreateTime(LocalDateTime.now());
        venue.setUpdateTime(LocalDateTime.now());
        venueMapper.insert(venue);
        return Result.success(venue);
    }

    @Override
    public Result updateVenue(Long id, VenueRequest request) {
        Venue venue = venueMapper.selectById(id);
        if (venue == null) {
            throw new BusinessException("场馆不存在");
        }
        BeanUtil.copyProperties(request, venue, "id", "adminUserId", "status", "rating", "reviewCount",
                "createTime", "updateTime");
        venue.setUpdateTime(LocalDateTime.now());
        venueMapper.updateById(venue);
        return Result.success(venue);
    }

    @Override
    public Result updateVenueStatus(Long id, String status) {
        Venue venue = venueMapper.selectById(id);
        if (venue == null) {
            throw new BusinessException("场馆不存在");
        }
        venue.setStatus(status);
        venue.setUpdateTime(LocalDateTime.now());
        venueMapper.updateById(venue);
        return Result.success("状态更新成功");
    }

    @Override
    public Result deleteVenue(Long id) {
        venueMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
