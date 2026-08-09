package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.CourseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseOrderMapper extends BaseMapper<CourseOrder> {

    @Select("SELECT * FROM course_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<CourseOrder> selectByUserId(Long userId);

    @Select("SELECT * FROM course_order WHERE course_id = #{courseId}")
    List<CourseOrder> selectByCourseId(Long courseId);
}
