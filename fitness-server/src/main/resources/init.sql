-- =============================================
-- FitPro 健身推荐系统 - 数据库初始化脚本
-- 数据库: fitness_recommend
-- 用法: mysql -u root -p < init.sql
-- =============================================

CREATE DATABASE IF NOT EXISTS fitness_recommend
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE fitness_recommend;

-- =============================================
-- 1. 系统用户表
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`      VARCHAR(50)  NOT NULL COMMENT '用户名',
  `password`      VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
  `nickname`      VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `email`         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar`        VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `gender`        TINYINT      DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  `age`           INT          DEFAULT NULL COMMENT '年龄',
  `height`        DECIMAL(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight`        DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `bmi`           DECIMAL(4,1) DEFAULT NULL COMMENT 'BMI',
  `fitness_goal`  VARCHAR(100) DEFAULT NULL COMMENT '健身目标',
  `exercise_frequency` VARCHAR(50) DEFAULT NULL COMMENT '锻炼频率',
  `role`          VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色 USER/ADMIN',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- =============================================
-- 2. 角色表
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name`   VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code`   VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- =============================================
-- 3. 用户-角色关联表
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- =============================================
-- 4. 场馆表 ★
-- =============================================
CREATE TABLE IF NOT EXISTS `venue` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '场馆ID',
  `name`            VARCHAR(100)  NOT NULL COMMENT '场馆名称',
  `address`         VARCHAR(255)  DEFAULT NULL COMMENT '地址',
  `phone`           VARCHAR(20)   DEFAULT NULL COMMENT '联系电话',
  `description`     TEXT          DEFAULT NULL COMMENT '场馆简介',
  `images`          TEXT          DEFAULT NULL COMMENT '图片(JSON数组)',
  `business_hours`  VARCHAR(100)  DEFAULT NULL COMMENT '营业时间',
  `facilities`      VARCHAR(500)  DEFAULT NULL COMMENT '设施标签(JSON数组)',
  `longitude`       DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
  `latitude`        DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
  `admin_user_id`   BIGINT        DEFAULT NULL COMMENT '管理员用户ID',
  `status`          VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '审核状态 PENDING/APPROVED/REJECTED',
  `rating`          DECIMAL(2,1)  NOT NULL DEFAULT 0.0 COMMENT '评分',
  `review_count`    INT           NOT NULL DEFAULT 0 COMMENT '评价数量',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_admin_user` (`admin_user_id`),
  KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场馆表';

-- =============================================
-- 5. 课程表
-- =============================================
CREATE TABLE IF NOT EXISTS `course` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `name`           VARCHAR(100)  NOT NULL COMMENT '课程名称',
  `description`    TEXT          DEFAULT NULL COMMENT '课程描述',
  `type`           VARCHAR(50)   DEFAULT NULL COMMENT '课程类型 YOGA/STRENGTH/CARDIO/DANCE/SWIMMING等',
  `difficulty`     VARCHAR(20)   DEFAULT NULL COMMENT '难度 BEGINNER/INTERMEDIATE/ADVANCED',
  `max_students`   INT           NOT NULL DEFAULT 20 COMMENT '最大人数',
  `enrolled_count` INT           NOT NULL DEFAULT 0 COMMENT '已报名人数',
  `coach_id`       BIGINT        DEFAULT NULL COMMENT '教练ID',
  `venue_id`       BIGINT        DEFAULT NULL COMMENT '场馆ID',
  `price`          DECIMAL(8,2)  NOT NULL DEFAULT 0.00 COMMENT '价格',
  `cover_image`    VARCHAR(255)  DEFAULT NULL COMMENT '封面图',
  `start_time`     DATETIME      DEFAULT NULL COMMENT '开始时间',
  `end_time`       DATETIME      DEFAULT NULL COMMENT '结束时间',
  `tags`           VARCHAR(300)  DEFAULT NULL COMMENT '标签(JSON数组)',
  `status`         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/CANCELLED/FINISHED',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_venue_id` (`venue_id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- =============================================
-- 6. 教练表
-- =============================================
CREATE TABLE IF NOT EXISTS `coach` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '教练ID',
  `name`         VARCHAR(50)   NOT NULL COMMENT '姓名',
  `avatar`       VARCHAR(255)  DEFAULT NULL COMMENT '头像',
  `gender`       TINYINT       DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  `age`          INT           DEFAULT NULL COMMENT '年龄',
  `experience`   VARCHAR(100)  DEFAULT NULL COMMENT '从业经验',
  `specialty`    VARCHAR(300)  DEFAULT NULL COMMENT '专长(JSON数组)',
  `cert_info`    VARCHAR(500)  DEFAULT NULL COMMENT '证书信息',
  `introduction` TEXT          DEFAULT NULL COMMENT '个人简介',
  `venue_id`     BIGINT        DEFAULT NULL COMMENT '所属场馆ID',
  `rating`       DECIMAL(2,1)  NOT NULL DEFAULT 0.0 COMMENT '评分',
  `review_count` INT           NOT NULL DEFAULT 0 COMMENT '评价数量',
  `status`       TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_venue_id` (`venue_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练表';

-- =============================================
-- 7. 评价表 ★（之前查询报500就是因为缺这个表）
-- =============================================
CREATE TABLE IF NOT EXISTS `review` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
  `target_type` VARCHAR(20)  NOT NULL COMMENT '目标类型 VENUE/COURSE/COACH',
  `target_id`   BIGINT       NOT NULL COMMENT '目标ID',
  `rating`      INT          NOT NULL COMMENT '评分 1-5',
  `content`     TEXT         DEFAULT NULL COMMENT '评价内容',
  `images`      TEXT         DEFAULT NULL COMMENT '图片(JSON数组)',
  `reply`       TEXT         DEFAULT NULL COMMENT '商家回复',
  `reply_time`  DATETIME     DEFAULT NULL COMMENT '回复时间',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- =============================================
-- 8. 课程订单表
-- =============================================
CREATE TABLE IF NOT EXISTS `course_order` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`    VARCHAR(32)  NOT NULL COMMENT '订单号',
  `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
  `course_id`   BIGINT       NOT NULL COMMENT '课程ID',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/PAID/CANCELLED/REFUNDED',
  `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程订单表';

-- =============================================
-- 9. 签到打卡表
-- =============================================
CREATE TABLE IF NOT EXISTS `check_in` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '打卡ID',
  `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
  `course_id`  BIGINT       DEFAULT NULL COMMENT '课程ID',
  `venue_id`   BIGINT       DEFAULT NULL COMMENT '场馆ID',
  `check_time` DATETIME     NOT NULL COMMENT '打卡时间',
  `note`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1有效 0取消',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_check_time` (`check_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到打卡表';

-- =============================================
-- 10. 收藏表
-- =============================================
CREATE TABLE IF NOT EXISTS `favorite` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '收藏类型 VENUE/COURSE/COACH/ARTICLE',
  `target_id`   BIGINT      NOT NULL COMMENT '目标ID',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- =============================================
-- 11. 用户行为表（推荐算法用）
-- =============================================
CREATE TABLE IF NOT EXISTS `user_behavior` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '行为ID',
  `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
  `behavior_type` VARCHAR(30)  NOT NULL COMMENT '行为类型 VIEW/CLICK/BOOKMARK/ENROLL/REVIEW',
  `target_type`   VARCHAR(20)  NOT NULL COMMENT '目标类型 VENUE/COURSE/COACH',
  `target_id`     BIGINT       NOT NULL COMMENT '目标ID',
  `target_name`   VARCHAR(100) DEFAULT NULL COMMENT '目标名称',
  `target_tags`   VARCHAR(500) DEFAULT NULL COMMENT '目标标签',
  `duration`      INT          DEFAULT 0 COMMENT '停留时长(秒)',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_behavior` (`behavior_type`, `target_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

-- =============================================
-- 12. 用户标签表（推荐画像用）
-- =============================================
CREATE TABLE IF NOT EXISTS `user_tag` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `user_id`     BIGINT        NOT NULL COMMENT '用户ID',
  `tag_name`    VARCHAR(50)   NOT NULL COMMENT '标签名',
  `tag_type`    VARCHAR(30)   NOT NULL COMMENT '标签类型 PREFERENCE/GOAL/LEVEL',
  `weight`      DECIMAL(5,2)  NOT NULL DEFAULT 1.0 COMMENT '权重',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  UNIQUE KEY `uk_user_tag` (`user_id`, `tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户标签表';

-- =============================================
-- 13. 社区文章表
-- =============================================
CREATE TABLE IF NOT EXISTS `article` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title`          VARCHAR(100) NOT NULL COMMENT '标题',
  `content`        LONGTEXT     DEFAULT NULL COMMENT '内容',
  `summary`        VARCHAR(300) DEFAULT NULL COMMENT '摘要',
  `cover`          VARCHAR(255) DEFAULT NULL COMMENT '封面图',
  `author_id`      BIGINT       NOT NULL COMMENT '作者ID',
  `category`       VARCHAR(50)  DEFAULT NULL COMMENT '分类',
  `tags`           VARCHAR(300) DEFAULT NULL COMMENT '标签(JSON数组)',
  `view_count`     INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  `like_count`     INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count`  INT          NOT NULL DEFAULT 0 COMMENT '评论数',
  `favorite_count` INT          NOT NULL DEFAULT 0 COMMENT '收藏数',
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态 DRAFT/PUBLISHED/BLOCKED',
  `is_top`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区文章表';

-- =============================================
-- 14. 文章点赞表
-- =============================================
CREATE TABLE IF NOT EXISTS `article_like` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `article_id`  BIGINT   NOT NULL COMMENT '文章ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_user` (`article_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章点赞表';

-- =============================================
-- 15. 文章评论表
-- =============================================
CREATE TABLE IF NOT EXISTS `article_comment` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id`       BIGINT       NOT NULL COMMENT '文章ID',
  `user_id`          BIGINT       NOT NULL COMMENT '用户ID',
  `content`          TEXT         NOT NULL COMMENT '评论内容',
  `parent_id`        BIGINT       DEFAULT NULL COMMENT '父评论ID(用于回复)',
  `reply_to_user_id` BIGINT       DEFAULT NULL COMMENT '回复目标用户ID',
  `like_count`       INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章评论表';

-- =============================================
-- 16. 系统公告表
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_announcement` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title`       VARCHAR(100) NOT NULL COMMENT '标题',
  `content`     TEXT         DEFAULT NULL COMMENT '内容',
  `creator_id`  BIGINT       DEFAULT NULL COMMENT '创建者ID',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1发布 0下架',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- =============================================
-- 初始数据
-- =============================================

-- 创建管理员账号 (密码: admin123)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2b$10$Lnd4uJhUHSxqfrUAj.qh..mOP3W3wpRpSfrcBjJypMupbSMeM0Yhm', 'System Admin', 'ADMIN');

-- 创建角色
INSERT INTO `sys_role` (`role_name`, `role_code`, `description`) VALUES
('管理员', 'ADMIN', '系统管理员，拥有所有权限'),
('普通用户', 'USER', '普通注册用户');

-- 关联管理员角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1);
