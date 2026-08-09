package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.SysAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysAnnouncementMapper extends BaseMapper<SysAnnouncement> {

    @Select("SELECT * FROM sys_announcement WHERE status = 1 ORDER BY create_time DESC")
    List<SysAnnouncement> selectActiveAnnouncements();
}
