package com.fitness.service;

import com.fitness.common.Result;

public interface CheckInService {
    Result checkIn(Long userId, Long courseId, Long venueId, String note);
    Result updateCheckIn(Long id, Long userId, Long venueId, String note);
    Result getMyCheckIns(Long userId, Integer page, Integer size);
    Result getTodayCheckInCount(Long userId);
    Result getCheckInStreak(Long userId);
    Result getCheckInRank(String type);
}