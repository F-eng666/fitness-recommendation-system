package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("SELECT * FROM review WHERE target_type = #{targetType} AND target_id = #{targetId} ORDER BY create_time DESC")
    List<Review> selectByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("SELECT AVG(rating) FROM review WHERE target_type = #{targetType} AND target_id = #{targetId}")
    Double selectAvgRating(@Param("targetType") String targetType, @Param("targetId") Long targetId);
}
