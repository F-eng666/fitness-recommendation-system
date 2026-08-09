package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.CoachRequest;

public interface CoachService {
    Result getCoachList(Integer page, Integer size, Long venueId, String keyword);
    Result getCoachDetail(Long id);
    Result createCoach(CoachRequest request);
    Result updateCoach(Long id, CoachRequest request);
    Result deleteCoach(Long id);
}