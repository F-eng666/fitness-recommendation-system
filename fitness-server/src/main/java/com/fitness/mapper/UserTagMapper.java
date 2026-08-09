package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.UserTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserTagMapper extends BaseMapper<UserTag> {

    @Select("SELECT * FROM user_tag WHERE user_id = #{userId}")
    List<UserTag> selectByUserId(Long userId);

    @Select("SELECT * FROM user_tag WHERE user_id = #{userId} AND tag_type = #{tagType}")
    List<UserTag> selectByUserIdAndType(@Param("userId") Long userId, @Param("tagType") String tagType);

    @Delete("DELETE FROM user_tag WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);
}
