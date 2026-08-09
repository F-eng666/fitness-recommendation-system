package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.CourseRequest;

public interface CourseService {
    Result getCourseList(Integer page, Integer size, String keyword, String type, String difficulty, Long venueId, Integer weekday, Integer published);
    Result getCourseDetail(Long id, Long userId);
    Result createCourse(CourseRequest request, Long adminUserId);
    Result updateCourse(Long id, CourseRequest request);
    Result deleteCourse(Long id);
    Result setRecurrence(Long id, String recurrence);
    Result getCourseDates(Long id);
    Result togglePublished(Long id);
}