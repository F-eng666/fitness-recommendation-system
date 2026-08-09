package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.BookingRequest;

public interface PersonalTrainingService {
    Result getAvailableSlots(Long coachId, Long venueId, String date);
    Result book(Long userId, BookingRequest request);
    Result getMyBookings(Long userId);
}
