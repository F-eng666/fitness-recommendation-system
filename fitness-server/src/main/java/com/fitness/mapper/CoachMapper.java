package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.Coach;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CoachMapper extends BaseMapper<Coach> {

    @Select("SELECT * FROM coach WHERE venue_id = #{venueId}")
    List<Coach> selectByVenueId(Long venueId);
}
