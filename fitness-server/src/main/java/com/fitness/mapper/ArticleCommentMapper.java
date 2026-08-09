package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.ArticleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleCommentMapper extends BaseMapper<ArticleComment> {

    @Select("SELECT * FROM article_comment WHERE article_id = #{articleId} AND parent_id IS NULL ORDER BY create_time DESC")
    List<ArticleComment> selectTopLevelByArticleId(Long articleId);

    @Select("SELECT * FROM article_comment WHERE parent_id = #{parentId} ORDER BY create_time ASC")
    List<ArticleComment> selectRepliesByParentId(Long parentId);
}
