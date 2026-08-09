package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.VenueRequest;

public interface VenueService {
    Result getVenueList(Integer page, Integer size, String keyword, String status);
    Result getMyVenues(Long adminUserId);
    Result getVenueDetail(Long id);
    Result createVenue(VenueRequest request, Long adminUserId);
    Result updateVenue(Long id, VenueRequest request);
    Result updateVenueStatus(Long id, String status);
    Result deleteVenue(Long id);
}