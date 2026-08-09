package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.AnnouncementRequest;

public interface AnnouncementService {
    Result getActiveAnnouncements();
    Result getAnnouncementList(Integer page, Integer size);
    Result createAnnouncement(Long creatorId, AnnouncementRequest request);
    Result updateAnnouncement(Long id, AnnouncementRequest request);
    Result deleteAnnouncement(Long id);
}