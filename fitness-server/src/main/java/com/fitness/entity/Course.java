package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String type;
    private String difficulty;

    @TableField("max_students")
    private Integer maxStudents;

    @TableField("enrolled_count")
    private Integer enrolledCount;

    @TableField("coach_id")
    private Long coachId;

    @TableField("venue_id")
    private Long venueId;

    private BigDecimal price;

    @TableField("cover_image")
    private String coverImage;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    private String tags;
    private String status;
    private String recurrence;

    @TableField("published")
    private Integer published;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
