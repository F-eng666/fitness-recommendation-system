package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.ArticleLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {

    @Select("SELECT * FROM article_like WHERE article_id = #{articleId} AND user_id = #{userId}")
    ArticleLike selectByArticleAndUser(Long articleId, Long userId);

    @Delete("DELETE FROM article_like WHERE article_id = #{articleId} AND user_id = #{userId}")
    int deleteByArticleAndUser(Long articleId, Long userId);
}
