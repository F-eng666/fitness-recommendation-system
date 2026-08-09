package com.fitness.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.AnnouncementRequest;
import com.fitness.entity.SysAnnouncement;
import com.fitness.mapper.SysAnnouncementMapper;
import com.fitness.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementServiceImpl implements AnnouncementService {

    private final SysAnnouncementMapper sysAnnouncementMapper;

    @Override
    public Result getActiveAnnouncements() {
        List<SysAnnouncement> announcements = sysAnnouncementMapper.selectActiveAnnouncements();
        return Result.success(announcements);
    }

    @Override
    public Result getAnnouncementList(Integer page, Integer size) {
        Page<SysAnnouncement> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysAnnouncement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysAnnouncement::getCreateTime);
        Page<SysAnnouncement> pageResult = sysAnnouncementMapper.selectPage(pageParam, wrapper);
        return Result.success(PageResult.of(pageResult));
    }

    @Override
    public Result createAnnouncement(Long creatorId, AnnouncementRequest request) {
        SysAnnouncement announcement = new SysAnnouncement();
        BeanUtil.copyProperties(request, announcement);
        announcement.setCreatorId(creatorId);
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        sysAnnouncementMapper.insert(announcement);
        return Result.success(announcement);
    }

    @Override
    public Result updateAnnouncement(Long id, AnnouncementRequest request) {
        SysAnnouncement announcement = sysAnnouncementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        BeanUtil.copyProperties(request, announcement, "id", "creatorId", "createTime", "updateTime");
        announcement.setUpdateTime(LocalDateTime.now());
        sysAnnouncementMapper.updateById(announcement);
        return Result.success(announcement);
    }

    @Override
    public Result deleteAnnouncement(Long id) {
        sysAnnouncementMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
