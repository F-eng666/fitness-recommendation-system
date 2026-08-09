package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.ReviewReplyRequest;
import com.fitness.dto.ReviewRequest;

public interface ReviewService {
    Result createReview(Long userId, ReviewRequest request);
    Result getTargetReviews(String targetType, Long targetId, Integer page, Integer size);
    Result replyReview(Long reviewId, ReviewReplyRequest request);
    Result deleteReview(Long reviewId, Long userId);
}