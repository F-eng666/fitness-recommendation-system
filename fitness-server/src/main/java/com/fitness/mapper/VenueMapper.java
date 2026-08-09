package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.Venue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VenueMapper extends BaseMapper<Venue> {

    @Select("SELECT * FROM venue WHERE admin_user_id = #{adminUserId}")
    List<Venue> selectByAdminUserId(Long adminUserId);
}
