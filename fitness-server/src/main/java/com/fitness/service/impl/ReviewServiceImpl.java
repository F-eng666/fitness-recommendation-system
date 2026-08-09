package com.fitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.ReviewReplyRequest;
import com.fitness.dto.ReviewRequest;
import com.fitness.entity.Review;
import com.fitness.mapper.ReviewMapper;
import com.fitness.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;

    @Override
    public Result createReview(Long userId, ReviewRequest request) {
        LambdaQueryWrapper<Review> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Review::getUserId, userId)
                .eq(Review::getTargetType, request.getTargetType())
                .eq(Review::getTargetId, request.getTargetId());
        Long existingCount = reviewMapper.selectCount(existWrapper);
        if (existingCount > 0) {
            throw new BusinessException("您已评价过该内容");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setTargetType(request.getTargetType());
        review.setTargetId(request.getTargetId());
        review.setRating(request.getRating().intValue());
        review.setContent(request.getContent());
        review.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(review);
        return Result.success(review);
    }

    @Override
    public Result getTargetReviews(String targetType, Long targetId, Integer page, Integer size) {
        Page<Review> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetType, targetType)
                .eq(Review::getTargetId, targetId)
                .orderByDesc(Review::getCreateTime);
        Page<Review> pageResult = reviewMapper.selectPage(pageParam, wrapper);
        return Result.success(PageResult.of(pageResult));
    }

    @Override
    public Result replyReview(Long reviewId, ReviewReplyRequest request) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        review.setReply(request.getReply());
        review.setReplyTime(LocalDateTime.now());
        reviewMapper.updateById(review);
        return Result.success(review);
    }

    @Override
    public Result deleteReview(Long reviewId, Long userId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该评价");
        }
        reviewMapper.deleteById(reviewId);
        return Result.success("删除成功");
    }
}