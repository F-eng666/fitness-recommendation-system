package com.fitness.config;

import com.fitness.entity.SysUser;
import com.fitness.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据初始化器 - 首次启动时创建默认管理员账号
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 检查管理员账号是否存在
        SysUser admin = sysUserMapper.selectByUsername("admin");
        if (admin == null) {
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("超级管理员");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            admin.setCreateTime(LocalDateTime.now());
            sysUserMapper.insert(admin);
            log.info("========================================");
            log.info("  默认管理员账号已创建");
            log.info("  用户名: admin  密码: admin123");
            log.info("========================================");
        }

        // 检查测试用户是否存在
        SysUser testUser = sysUserMapper.selectByUsername("testuser");
        if (testUser == null) {
            testUser = new SysUser();
            testUser.setUsername("testuser");
            testUser.setPassword(passwordEncoder.encode("user123"));
            testUser.setNickname("健身小白");
            testUser.setPhone("13800138000");
            testUser.setRole("USER");
            testUser.setStatus(1);
            testUser.setHeight(new java.math.BigDecimal("175.00"));
            testUser.setWeight(new java.math.BigDecimal("75.00"));
            testUser.setFitnessGoal("BUILD_MUSCLE");
            testUser.setExerciseFrequency("EVERY_OTHER_DAY");
            testUser.setCreateTime(LocalDateTime.now());
            sysUserMapper.insert(testUser);
            log.info("  默认测试用户已创建");
            log.info("  用户名: testuser  密码: user123");
        }

        log.info("========================================");
    }
}
