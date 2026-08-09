package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String summary;
    private String cover;

    @TableField("author_id")
    private Long authorId;

    private String category;
    private String tags;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("favorite_count")
    private Integer favoriteCount;

    private String status;

    @TableField("is_top")
    private Integer isTop;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
