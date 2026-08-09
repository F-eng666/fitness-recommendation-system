package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.AIPlanRequest;

public interface AIService {
    Result generatePlan(AIPlanRequest request, Long userId);
}