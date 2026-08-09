package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.BehaviorRecordRequest;

public interface RecommendService {
    Result getRecommendedCourses(Long userId, Integer limit);
    Result getRecommendedCoaches(Long userId, Integer limit);
    Result recordBehavior(Long userId, BehaviorRecordRequest request);
    Result updateUserTags(Long userId);
}