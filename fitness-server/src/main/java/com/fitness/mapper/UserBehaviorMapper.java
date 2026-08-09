package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    @Select("SELECT * FROM user_behavior WHERE user_id = #{userId} AND create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) ORDER BY create_time DESC")
    List<UserBehavior> selectRecent30Days(Long userId);
}
