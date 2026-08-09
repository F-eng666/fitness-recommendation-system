package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.CheckIn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CheckInMapper extends BaseMapper<CheckIn> {

    @Select("SELECT * FROM check_in WHERE user_id = #{userId} ORDER BY check_time DESC")
    List<CheckIn> selectByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM check_in WHERE user_id = #{userId} AND DATE(check_time) = CURDATE()")
    int countTodayCheckIn(Long userId);
}
