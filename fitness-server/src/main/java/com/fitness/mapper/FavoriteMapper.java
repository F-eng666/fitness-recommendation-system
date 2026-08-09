package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.Favorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Select("SELECT * FROM favorite WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Favorite> selectByUserId(Long userId);

    @Select("SELECT * FROM favorite WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    Favorite selectByUserAndTarget(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Delete("DELETE FROM favorite WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int deleteByUserAndTarget(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);
}
