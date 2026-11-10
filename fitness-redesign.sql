-- ============================================
-- 健身推荐管理系统 数据库初始化脚本（重设计版）
-- Database: MySQL 8.0
-- 设计原则：
--   1. 高内聚低耦合：课程(course)/客户(customer)/教练(coach)/场馆(venue) 四张独立基础表
--   2. 统一预约表 booking：以时间信息关联客户、课程、教练、场馆
--   3. 命名大众化：订单表 order、交易表 transaction、公告表 notice、标签表 tag、行为表 behavior
-- ============================================

CREATE DATABASE IF NOT EXISTS fitness_recommend DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fitness_recommend;

-- ============================================
-- 1. 客户表（原 sys_user）
-- ============================================
DROP TABLE IF EXISTS `customer`;
CREATE TABLE `customer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号（客户唯一账号）',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `age` INT DEFAULT NULL COMMENT '年龄',
    `height` DECIMAL(5,2) DEFAULT NULL COMMENT '身高(cm)，AI计划输入',
    `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)，AI计划输入',
    `bmi` DECIMAL(5,2) DEFAULT NULL COMMENT 'BMI值，AI计划输入',
    `fitness_goal` VARCHAR(50) DEFAULT NULL COMMENT '健身目标：LOSE_WEIGHT/BUILD_MUSCLE/SHAPE/GAIN_WEIGHT/KEEP_HEALTHY',
    `exercise_frequency` VARCHAR(50) DEFAULT NULL COMMENT '运动频率：DAILY/EVERY_OTHER_DAY/TWICE_WEEK/WEEKLY/RARELY',
    `role` VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' COMMENT '角色：CUSTOMER/VENUE_ADMIN/ADMIN',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- ============================================
-- 2. 角色表（原 sys_role）
-- ============================================
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ============================================
-- 3. 客户角色关联表（原 sys_user_role）
-- ============================================
DROP TABLE IF EXISTS `customer_role`;
CREATE TABLE `customer_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户角色关联表';

-- ============================================
-- 4. 场馆表（基础表）
-- ============================================
DROP TABLE IF EXISTS `venue`;
CREATE TABLE `venue` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '场馆名称',
    `address` VARCHAR(255) NOT NULL COMMENT '场馆地址',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `description` TEXT COMMENT '场馆介绍',
    `images` TEXT COMMENT '场馆图片（JSON数组URL）',
    `business_hours` VARCHAR(100) DEFAULT NULL COMMENT '营业时间',
    `facilities` VARCHAR(500) DEFAULT NULL COMMENT '设施标签（逗号分隔）',
    `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
    `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
    `admin_user_id` BIGINT DEFAULT NULL COMMENT '场馆管理员ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
    `rating` DECIMAL(3,2) DEFAULT 0.00 COMMENT '平均评分',
    `review_count` INT DEFAULT 0 COMMENT '评价数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_user_id` (`admin_user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场馆表';

-- ============================================
-- 5. 教练表（基础表）
-- ============================================
DROP TABLE IF EXISTS `coach`;
CREATE TABLE `coach` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `age` INT DEFAULT NULL COMMENT '年龄',
    `experience` VARCHAR(100) DEFAULT NULL COMMENT '执教经验（如"5年"）',
    `specialty` VARCHAR(500) DEFAULT NULL COMMENT '擅长领域（逗号分隔）',
    `cert_info` TEXT COMMENT '证书信息（JSON）',
    `introduction` TEXT COMMENT '个人简介',
    `venue_id` BIGINT NOT NULL COMMENT '所属场馆ID',
    `rating` DECIMAL(3,2) DEFAULT 0.00 COMMENT '平均评分',
    `review_count` INT DEFAULT 0 COMMENT '评价数量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_venue_id` (`venue_id`),
    KEY `idx_specialty` (`specialty`(255)),
    KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练表';

-- ============================================
-- 6. 课程表（基础表）
-- ============================================
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `description` TEXT COMMENT '课程描述',
    `type` VARCHAR(50) NOT NULL COMMENT '课程类型：YOGA/STRENGTH/CARDIO/DANCE/SWIMMING/BASKETBALL/BOXING/PILATES/SPINNING/OTHER',
    `difficulty` VARCHAR(20) DEFAULT 'BEGINNER' COMMENT '难度：BEGINNER/INTERMEDIATE/ADVANCED',
    `max_students` INT NOT NULL DEFAULT 20 COMMENT '最大容量',
    `enrolled_count` INT DEFAULT 0 COMMENT '已报名人数',
    `coach_id` BIGINT DEFAULT NULL COMMENT '教练ID',
    `venue_id` BIGINT NOT NULL COMMENT '场馆ID',
    `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '价格',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
    `start_time` DATETIME NOT NULL COMMENT '课程开始时间',
    `end_time` DATETIME NOT NULL COMMENT '课程结束时间',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UPCOMING' COMMENT '状态：UPCOMING/ONGOING/FINISHED/CANCELLED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_venue_id` (`venue_id`),
    KEY `idx_coach_id` (`coach_id`),
    KEY `idx_type` (`type`),
    KEY `idx_difficulty` (`difficulty`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ============================================
-- 7. 预约表（统一关联客户/课程/教练/场馆，原 course_order + personal_training）
-- ============================================
DROP TABLE IF EXISTS `booking`;
CREATE TABLE `booking` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(32) NOT NULL COMMENT '预约单号，唯一',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID（customer.id）',
    `course_id` BIGINT DEFAULT NULL COMMENT '课程ID，团课预约时填写，私教预约为空',
    `coach_id` BIGINT DEFAULT NULL COMMENT '教练ID，私教预约时填写，团课预约为空',
    `venue_id` BIGINT NOT NULL COMMENT '场馆ID（venue.id）',
    `start_time` DATETIME NOT NULL COMMENT '预约开始时间（课程开始时间/私教开始时间）',
    `end_time` DATETIME NOT NULL COMMENT '预约结束时间',
    `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预约金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CONFIRMED/CANCELLED/COMPLETED',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_coach_id` (`coach_id`),
    KEY `idx_venue_id` (`venue_id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- ============================================
-- 8. 订单表（支付订单，一对一关联预约）
-- ============================================
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号，唯一',
    `booking_id` BIGINT NOT NULL COMMENT '预约ID（booking.id），一对一关联',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `pay_method` VARCHAR(20) NOT NULL DEFAULT 'WECHAT' COMMENT '支付方式：WECHAT/ALIPAY/WALLET',
    `pay_status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态：UNPAID/PAID/REFUNDING/REFUNDED',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_booking_id` (`booking_id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================
-- 9. 钱包表
-- ============================================
DROP TABLE IF EXISTS `wallet`;
CREATE TABLE `wallet` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '余额',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包表';

-- ============================================
-- 10. 交易表（原 wallet_transaction）
-- ============================================
DROP TABLE IF EXISTS `transaction`;
CREATE TABLE `transaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `wallet_id` BIGINT NOT NULL COMMENT '钱包ID（wallet.id）',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `type` VARCHAR(20) NOT NULL COMMENT '交易类型：INCOME/EXPENSE',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '交易金额',
    `balance_before` DECIMAL(10,2) NOT NULL COMMENT '交易前余额',
    `balance_after` DECIMAL(10,2) NOT NULL COMMENT '交易后余额',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID（order.id），充值/支付/退款时填写',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '交易说明',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
    PRIMARY KEY (`id`),
    KEY `idx_wallet_id` (`wallet_id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易表';

-- ============================================
-- 11. 会员卡模板表（原 membership_card）
-- ============================================
DROP TABLE IF EXISTS `member_card`;
CREATE TABLE `member_card` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `venue_id` BIGINT NOT NULL COMMENT '所属场馆ID',
    `name` VARCHAR(50) NOT NULL COMMENT '卡片名称',
    `card_type` VARCHAR(20) NOT NULL COMMENT '类型：SESSION/WEEKLY/MONTHLY/QUARTERLY',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
    `duration_days` INT DEFAULT NULL COMMENT '有效天数（周卡=7，月卡=30，季卡=90）',
    `session_count` INT DEFAULT NULL COMMENT '次数（次卡专用）',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '卡片描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架 1上架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_venue_id` (`venue_id`),
    KEY `idx_card_type` (`card_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员卡模板表';

-- ============================================
-- 12. 客户会员卡表（原 user_membership）
-- ============================================
DROP TABLE IF EXISTS `member`;
CREATE TABLE `member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `card_id` BIGINT NOT NULL COMMENT '卡模板ID（member_card.id）',
    `venue_id` BIGINT NOT NULL COMMENT '场馆ID',
    `card_type` VARCHAR(20) NOT NULL COMMENT '卡类型',
    `order_id` BIGINT NOT NULL COMMENT '购买订单ID（order.id）',
    `price` DECIMAL(10,2) NOT NULL COMMENT '购买价格',
    `remaining_sessions` INT DEFAULT NULL COMMENT '剩余次数（次卡）',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间（周/月/季卡）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/EXPIRED/USED_UP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开通时间',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_venue_id` (`venue_id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户会员卡表';

-- ============================================
-- 13. 文章表
-- ============================================
DROP TABLE IF EXISTS `article`;
CREATE TABLE `article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
    `content` LONGTEXT COMMENT '文章内容（Markdown）',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
    `cover` VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
    `author_id` BIGINT NOT NULL COMMENT '作者ID',
    `category` VARCHAR(50) NOT NULL COMMENT '分类：BEGINNER/NUTRITION/TECHNIQUE/EXPERT_SHARE/INJURY_PREVENTION',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `favorite_count` INT DEFAULT 0 COMMENT '收藏数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：DRAFT/PUBLISHED/HIDDEN',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶：0否 1是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    FULLTEXT KEY `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- ============================================
-- 14. 文章评论表
-- ============================================
DROP TABLE IF EXISTS `article_comment`;
CREATE TABLE `article_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `customer_id` BIGINT NOT NULL COMMENT '评论客户ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID（二级回复）',
    `reply_to_customer_id` BIGINT DEFAULT NULL COMMENT '被回复的客户ID',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章评论表';

-- ============================================
-- 15. 文章点赞表
-- ============================================
DROP TABLE IF EXISTS `article_like`;
CREATE TABLE `article_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_customer` (`article_id`, `customer_id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章点赞表';

-- ============================================
-- 16. 公告表（原 sys_announcement）
-- ============================================
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告内容',
    `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0隐藏 1显示',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ============================================
-- 17. 打卡签到表
-- ============================================
DROP TABLE IF EXISTS `check_in`;
CREATE TABLE `check_in` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `course_id` BIGINT DEFAULT NULL COMMENT '课程ID（可为空，自由训练打卡）',
    `venue_id` BIGINT DEFAULT NULL COMMENT '场馆ID',
    `check_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
    `note` VARCHAR(255) DEFAULT NULL COMMENT '打卡备注',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0无效 1有效',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_check_time` (`check_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡签到表';

-- ============================================
-- 18. 评价表
-- ============================================
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `target_type` VARCHAR(20) NOT NULL COMMENT '评价目标：VENUE/COURSE/COACH',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `rating` INT NOT NULL DEFAULT 5 COMMENT '评分 1-5',
    `content` TEXT COMMENT '评价内容',
    `images` TEXT COMMENT '评价图片（JSON数组URL）',
    `reply` TEXT COMMENT '管理员/场馆回复',
    `reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ============================================
-- 19. 收藏表
-- ============================================
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `target_type` VARCHAR(20) NOT NULL COMMENT '收藏目标：VENUE/COURSE/COACH/ARTICLE',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_target` (`customer_id`, `target_type`, `target_id`),
    KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ============================================
-- 20. 客户标签表（原 user_tag）
-- ============================================
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名',
    `tag_type` VARCHAR(20) NOT NULL COMMENT '标签类型：BODY/GOAL/BEHAVIOR',
    `weight` DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '标签权重，默认1.00',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_tag` (`customer_id`, `tag_name`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_tag_type` (`tag_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户标签表';

-- ============================================
-- 21. 客户行为表（原 user_behavior）
-- ============================================
DROP TABLE IF EXISTS `behavior`;
CREATE TABLE `behavior` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `behavior_type` VARCHAR(20) NOT NULL COMMENT '行为类型：BROWSE/BOOK/LIKE/COMMENT/CHECK_IN/FAVORITE',
    `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型：VENUE/COURSE/COACH/ARTICLE',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `target_name` VARCHAR(100) DEFAULT NULL COMMENT '目标名称（快照）',
    `target_tags` VARCHAR(500) DEFAULT NULL COMMENT '目标标签（快照，用于画像计算）',
    `duration` INT DEFAULT 0 COMMENT '浏览时长（秒）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_customer_behavior` (`customer_id`, `behavior_type`, `create_time`),
    KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户行为表';

-- ============================================
-- 初始化数据
-- ============================================

INSERT INTO `role` (`role_name`, `role_code`, `description`) VALUES
('普通客户', 'CUSTOMER', '普通会员客户'),
('场馆管理员', 'VENUE_ADMIN', '场馆管理员'),
('管理员', 'ADMIN', '系统管理员');

INSERT INTO `venue` (`name`, `address`, `phone`, `description`, `facilities`, `status`, `admin_user_id`) VALUES
('力量工厂健身中心', '北京市朝阳区建国路88号', '010-88886666', '拥有国际顶级健身器材，2000平超大空间，配备恒温游泳池', '跑步机,椭圆机,史密斯架,哑铃区,游泳池,瑜伽室,动感单车', 'APPROVED', 2),
('灵动瑜伽馆', '北京市海淀区中关村大街16号', '010-66668888', '专注瑜伽教学8年，拥有多位国际认证瑜伽导师', '瑜伽垫,瑜伽球,空中瑜伽,冥想室', 'APPROVED', 2),
('燃脂搏击俱乐部', '北京市西城区金融街8号', '010-99995555', '高强度燃脂训练，专业搏击教练团队', '沙袋,拳击台,跳绳区,力量区', 'PENDING', 2);

INSERT INTO `coach` (`name`, `gender`, `age`, `experience`, `specialty`, `introduction`, `venue_id`) VALUES
('张伟', 1, 32, '8年', '力量训练,增肌塑形,体能训练', '前国家队体能教练，擅长力量训练与增肌塑形', 1),
('李娜', 2, 28, '5年', '瑜伽,普拉提,空中瑜伽,孕产瑜伽', 'RYT-500认证瑜伽导师，曾赴印度深造', 2),
('王强', 1, 30, '6年', '搏击,拳击,MUAYTHAI,HIIT', '前职业拳击手，3年执教经验', 3),
('陈芳', 2, 25, '3年', '动感单车,有氧操,ZUMBA,减脂训练', 'LES MILLS认证教练，课堂氛围活跃', 1);

INSERT INTO `course` (`name`, `description`, `type`, `difficulty`, `max_students`, `coach_id`, `venue_id`, `price`, `start_time`, `end_time`, `tags`, `status`) VALUES
('高强度增肌训练', '针对上半身的增肌训练计划，包含卧推、引体向上等核心动作', 'STRENGTH', 'ADVANCED', 15, 1, 1, 199.00, '2026-11-10 09:00:00', '2026-11-10 10:30:00', '增肌,力量,上半身', 'UPCOMING'),
('流瑜伽入门', '适合零基础学员的流瑜伽课程，注重呼吸与动作的协调', 'YOGA', 'BEGINNER', 25, 2, 2, 99.00, '2026-11-10 10:00:00', '2026-11-10 11:00:00', '瑜伽,入门,柔韧', 'UPCOMING'),
('燃脂搏击操', '结合拳击和有氧的高强度燃脂课程', 'BOXING', 'INTERMEDIATE', 20, 3, 3, 149.00, '2026-11-11 14:00:00', '2026-11-11 15:00:00', '燃脂,搏击,HIIT', 'UPCOMING'),
('动感单车燃脂派对', '跟着音乐节奏一起燃烧卡路里', 'SPINNING', 'BEGINNER', 30, 4, 1, 79.00, '2026-11-11 19:00:00', '2026-11-11 20:00:00', '燃脂,单车,音乐', 'UPCOMING'),
('普拉提核心训练', '增强核心力量，改善体态', 'PILATES', 'INTERMEDIATE', 20, 2, 2, 129.00, '2026-11-12 08:00:00', '2026-11-12 09:00:00', '普拉提,核心,体态', 'UPCOMING');

INSERT INTO `article` (`title`, `content`, `summary`, `author_id`, `category`, `tags`, `view_count`, `like_count`, `comment_count`, `status`) VALUES
('新手健身入门完全指南', '# 新手健身入门完全指南\n\n## 第一步：明确你的目标\n\n健身之前，首先要明确自己的目标：\n- **减脂**：以有氧运动为主\n- **增肌**：以力量训练为主\n- **塑形**：力量+有氧结合\n\n## 第二步：制定训练计划\n\n建议新手从每周3次开始...\n\n## 第三步：饮食搭配\n\n三分练七分吃...', '为刚接触健身的小白准备的入门指南，包含目标设定、训练计划和饮食建议', 1, 'BEGINNER', '入门,新手,指南', 1520, 128, 35, 'PUBLISHED'),
('增肌期的蛋白质摄入指南', '# 增肌期的蛋白质摄入指南\n\n增肌期蛋白质摄入至关重要...', '详细讲解增肌期间如何科学补充蛋白质', 1, 'NUTRITION', '增肌,蛋白质,饮食', 980, 76, 22, 'PUBLISHED'),
('瑜伽呼吸法：提升练习效果的关键', '# 瑜伽呼吸法\n\n瑜伽不仅仅是体式...\n\n掌握正确的呼吸方法，让瑜伽练习事半功倍', '掌握正确的呼吸方法，让瑜伽练习事半功倍', 1, 'TECHNIQUE', '瑜伽,呼吸,技巧', 620, 45, 12, 'PUBLISHED');

INSERT INTO `member_card` (`venue_id`, `name`, `card_type`, `price`, `duration_days`, `session_count`, `description`, `status`) VALUES
(1, '力量工厂·次卡', 'SESSION', 39.00, NULL, 1, '单次体验卡，畅享全部器械，有效期30天', 1),
(1, '力量工厂·周卡', 'WEEKLY', 199.00, 7, NULL, '7天无限次使用，含团体课程', 1),
(1, '力量工厂·月卡', 'MONTHLY', 599.00, 30, NULL, '30天无限次使用，含团体课程+游泳池', 1),
(1, '力量工厂·季卡', 'QUARTERLY', 1499.00, 90, NULL, '90天无限次使用，含全部课程+私教体验1次', 1),
(2, '灵动瑜伽·次卡', 'SESSION', 49.00, NULL, 1, '单次瑜伽体验，任选一节团体课', 1),
(2, '灵动瑜伽·周卡', 'WEEKLY', 259.00, 7, NULL, '7天无限次瑜伽课程', 1),
(2, '灵动瑜伽·月卡', 'MONTHLY', 699.00, 30, NULL, '30天无限次瑜伽+普拉提课程', 1),
(2, '灵动瑜伽·季卡', 'QUARTERLY', 1799.00, 90, NULL, '90天无限次课程+每月1次私教指导', 1),
(3, '燃脂搏击·次卡', 'SESSION', 59.00, NULL, 1, '单次搏击体验课，燃脂暴汗', 1),
(3, '燃脂搏击·周卡', 'WEEKLY', 299.00, 7, NULL, '7天无限次搏击课程', 1),
(3, '燃脂搏击·月卡', 'MONTHLY', 799.00, 30, NULL, '30天无限次搏击+HIIT课程', 1),
(3, '燃脂搏击·季卡', 'QUARTERLY', 1999.00, 90, NULL, '90天无限次课程+拳击装备一套', 1);

INSERT INTO `notice` (`title`, `content`, `creator_id`, `status`) VALUES
('欢迎使用健身推荐系统', '欢迎各位健身爱好者！本系统提供智能课程推荐、AI定制训练计划、健身知识社区等功能，祝您健身愉快！', 1, 1),
('系统V1.0正式上线', '健身推荐管理系统V1.0版本正式上线，包含用户画像分析、个性化推荐、AI训练计划生成等核心功能', 1, 1);
