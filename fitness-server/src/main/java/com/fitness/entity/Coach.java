package com.fitness.entity;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@TableName("coach")
public class Coach {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String avatar;
    private Integer gender;
    private Integer age;
    private String experience;
    private String specialty;

    @TableField(exist = false)
    private List<String> specialtyList;

    public List<String> getSpecialtyList() {
        if (specialtyList != null) {
            return specialtyList;
        }
        if (specialty != null && !specialty.isEmpty()) {
            try {
                return JSONUtil.toList(specialty, String.class);
            } catch (Exception e) {
                return Collections.singletonList(specialty);
            }
        }
        return Collections.emptyList();
    }

    @TableField("cert_info")
    private String certInfo;

    private String introduction;

    @TableField("venue_id")
    private Long venueId;

    private BigDecimal rating;

    @TableField("review_count")
    private Integer reviewCount;

    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
