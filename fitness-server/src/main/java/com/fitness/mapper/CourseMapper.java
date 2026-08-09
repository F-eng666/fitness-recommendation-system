package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    @Select("SELECT * FROM course WHERE venue_id = #{venueId}")
    List<Course> selectByVenueId(Long venueId);

    @Select("SELECT * FROM course WHERE coach_id = #{coachId}")
    List<Course> selectByCoachId(Long coachId);
}
