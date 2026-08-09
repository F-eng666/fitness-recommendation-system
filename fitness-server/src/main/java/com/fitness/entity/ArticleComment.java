package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_comment")
public class ArticleComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("article_id")
    private Long articleId;

    @TableField("user_id")
    private Long userId;

    private String content;

    @TableField("parent_id")
    private Long parentId;

    @TableField("reply_to_user_id")
    private Long replyToUserId;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("create_time")
    private LocalDateTime createTime;
}
