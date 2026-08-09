package com.fitness.service;

import com.fitness.common.Result;

import java.util.List;
import java.util.Map;

public interface MembershipService {
    Result getCardsByVenue(Long venueId);
    Result getHotCards();
    Result purchaseCard(Long cardId, Long venueId, Long userId);
    Result getMyCards(Long userId, Integer page, Integer size);
    Result saveVenueCards(Long venueId, List<Map<String, Object>> cards);
}
