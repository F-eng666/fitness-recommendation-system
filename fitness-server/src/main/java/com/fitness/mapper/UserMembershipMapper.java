package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.UserMembership;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMembershipMapper extends BaseMapper<UserMembership> {

    @Select("SELECT * FROM user_membership WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<UserMembership> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_membership WHERE user_id = #{userId} AND venue_id = #{venueId} AND status = 'ACTIVE' ORDER BY create_time DESC")
    List<UserMembership> selectActiveByUserAndVenue(@Param("userId") Long userId, @Param("venueId") Long venueId);
}
