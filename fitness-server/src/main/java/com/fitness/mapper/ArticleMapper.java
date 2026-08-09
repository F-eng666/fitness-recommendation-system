package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Select("SELECT * FROM article WHERE author_id = #{authorId} ORDER BY create_time DESC")
    List<Article> selectByAuthorId(Long authorId);

    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);

    @Update("UPDATE article SET like_count = like_count + #{delta} WHERE id = #{id}")
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE article SET comment_count = comment_count + #{delta} WHERE id = #{id}")
    int updateCommentCount(@Param("id") Long id, @Param("delta") int delta);
}
