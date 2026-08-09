package com.fitness.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.ChangePasswordRequest;
import com.fitness.dto.LoginRequest;
import com.fitness.dto.RegisterRequest;
import com.fitness.dto.UserProfileRequest;
import com.fitness.entity.SysUser;
import com.fitness.entity.UserTag;
import com.fitness.entity.Wallet;
import com.fitness.enums.TagTypeEnum;
import com.fitness.mapper.SysUserMapper;
import com.fitness.mapper.UserTagMapper;
import com.fitness.mapper.WalletMapper;
import com.fitness.security.JwtTokenProvider;
import com.fitness.service.UserService;
import com.fitness.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final UserTagMapper userTagMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final WalletMapper walletMapper;
    private final WalletService walletService;

    @Override
    public Result login(LoginRequest request) {
        SysUser user = sysUserMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        user.setPassword(null);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return Result.success(result);
    }

    @Override
    public Result register(RegisterRequest request) {
        SysUser existUser = sysUserMapper.selectByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole() != null ? request.getRole() : "USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);

        updateBodyTags(user);
        updateGoalTags(user);

        // 创建钱包
        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCreateTime(LocalDateTime.now());
        wallet.setUpdateTime(LocalDateTime.now());
        walletMapper.insert(wallet);
        log.info("Created wallet for new user: {}", user.getId());

        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result getUserProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result updateUserProfile(Long userId, UserProfileRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        BeanUtil.copyProperties(request, user, "id", "username", "password", "role", "status",
                "createTime", "updateTime");

        if (request.getHeight() != null && request.getWeight() != null
                && request.getHeight().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal heightInMeters = request.getHeight().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal bmi = request.getWeight().divide(
                    heightInMeters.multiply(heightInMeters), 2, RoundingMode.HALF_UP);
            user.setBmi(bmi);
            updateBodyTags(user);
        }

        if (StringUtils.hasText(request.getFitnessGoal())) {
            updateGoalTags(user);
        }

        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return Result.success("密码修改成功");
    }

    @Override
    public Result getUserList(Integer page, Integer size, String keyword) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> pageResult = sysUserMapper.selectPage(pageParam, wrapper);
        pageResult.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(PageResult.of(pageResult));
    }

    @Override
    public Result updateUserStatus(Long userId, Integer status) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return Result.success("状态更新成功");
    }

    private void updateBodyTags(SysUser user) {
        LambdaQueryWrapper<UserTag> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserTag::getUserId, user.getId())
                .eq(UserTag::getTagType, TagTypeEnum.BODY.getCode());
        userTagMapper.delete(deleteWrapper);

        if (user.getBmi() == null) {
            return;
        }
        BigDecimal bmi = user.getBmi();
        String tagName;
        if (bmi.compareTo(new BigDecimal("18.5")) < 0) {
            tagName = "体型-偏瘦";
        } else if (bmi.compareTo(new BigDecimal("24")) < 0) {
            tagName = "体型-标准";
        } else if (bmi.compareTo(new BigDecimal("28")) < 0) {
            tagName = "体型-偏胖";
        } else {
            tagName = "体型-肥胖";
        }
        UserTag tag = new UserTag();
        tag.setUserId(user.getId());
        tag.setTagName(tagName);
        tag.setTagType(TagTypeEnum.BODY.getCode());
        tag.setWeight(new BigDecimal("1.0"));
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        userTagMapper.insert(tag);
    }

    private void updateGoalTags(SysUser user) {
        LambdaQueryWrapper<UserTag> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserTag::getUserId, user.getId())
                .eq(UserTag::getTagType, TagTypeEnum.GOAL.getCode());
        userTagMapper.delete(deleteWrapper);

        if (!StringUtils.hasText(user.getFitnessGoal())) {
            user.setFitnessGoal("KEEP_HEALTHY");
        }
        String tagName = mapGoalToTag(user.getFitnessGoal());
        UserTag tag = new UserTag();
        tag.setUserId(user.getId());
        tag.setTagName(tagName);
        tag.setTagType(TagTypeEnum.GOAL.getCode());
        tag.setWeight(new BigDecimal("3.0"));
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        userTagMapper.insert(tag);
    }

    private String mapGoalToTag(String fitnessGoal) {
        if (fitnessGoal == null) {
            return "目标-保持健康";
        }
        switch (fitnessGoal) {
            case "LOSE_WEIGHT":
                return "目标-减脂";
            case "BUILD_MUSCLE":
                return "目标-增肌";
            case "SHAPE":
                return "目标-塑形";
            case "GAIN_WEIGHT":
                return "目标-增重";
            case "KEEP_HEALTHY":
            default:
                return "目标-保持健康";
        }
    }
}
