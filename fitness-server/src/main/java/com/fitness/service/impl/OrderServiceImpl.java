package com.fitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.OrderRequest;
import com.fitness.entity.*;
import com.fitness.mapper.*;
import com.fitness.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final CourseOrderMapper courseOrderMapper;
    private final CourseMapper courseMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final VenueMapper venueMapper;
    private final SysUserMapper sysUserMapper;
    private final CoachMapper coachMapper;
    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    @Override
    public Result createOrder(Long userId, OrderRequest request) {
        Course course = courseMapper.selectById(request.getCourseId());
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!"UPCOMING".equals(course.getStatus())) {
            throw new BusinessException("该课程当前不可预约");
        }
        if (course.getEnrolledCount() >= course.getMaxStudents()) {
            throw new BusinessException("该课程已满员");
        }
        LambdaQueryWrapper<CourseOrder> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(CourseOrder::getUserId, userId)
                .eq(CourseOrder::getCourseId, request.getCourseId())
                .in(CourseOrder::getStatus, "PENDING", "CONFIRMED");
        Long existingCount = courseOrderMapper.selectCount(existWrapper);
        if (existingCount > 0) {
            throw new BusinessException("您已预约过该课程");
        }

        // 检测钱包余额
        java.math.BigDecimal price = course.getPrice() != null ? course.getPrice() : java.math.BigDecimal.ZERO;
        Wallet wallet = walletMapper.selectByUserId(userId);
        java.math.BigDecimal balance = wallet != null ? wallet.getBalance() : java.math.BigDecimal.ZERO;
        if (balance.compareTo(price) < 0) {
            Map<String, Object> info = new HashMap<>();
            info.put("balance", balance);
            info.put("price", price);
            info.put("shortage", price.subtract(balance));
            throw new BusinessException(402, "余额不足，需要 ¥" + price + "，当前余额 ¥" + balance + "，请先充值");
        }

        // 扣款
        wallet.setBalance(balance.subtract(price));
        wallet.setUpdateTime(LocalDateTime.now());
        walletMapper.updateById(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setUserId(userId);
        tx.setType("PAY");
        tx.setAmount(price.negate());
        tx.setBalanceBefore(balance);
        tx.setBalanceAfter(wallet.getBalance());
        tx.setRemark("预约课程：" + course.getName());
        tx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(tx);

        CourseOrder order = new CourseOrder();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setUserId(userId);
        order.setCourseId(request.getCourseId());
        order.setRemark(request.getRemark());
        order.setStatus("PAID");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        courseOrderMapper.insert(order);

        course.setEnrolledCount(course.getEnrolledCount() + 1);
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);

        recordBehavior(userId, "BOOK", "COURSE", course.getId(), course.getName(), course.getTags());

        Map<String, Object> resp = new HashMap<>();
        resp.put("order", order);
        resp.put("balanceAfter", wallet.getBalance());
        return Result.success(resp);
    }

    @Override
    public Result getMyOrders(Long userId, Integer page, Integer size) {
        completeExpiredOrders(userId);
        Page<CourseOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CourseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseOrder::getUserId, userId)
                .orderByDesc(CourseOrder::getCreateTime);
        Page<CourseOrder> pageResult = courseOrderMapper.selectPage(pageParam, wrapper);
        return Result.success(enrichOrders(pageResult));
    }

    @Override
    public Result getVenueOrders(Long adminUserId, Integer page, Integer size) {
        List<Venue> venues = venueMapper.selectByAdminUserId(adminUserId);
        if (venues.isEmpty()) {
            return Result.success(PageResult.of(0L, Collections.emptyList()));
        }
        List<Long> venueIds = venues.stream().map(Venue::getId).collect(Collectors.toList());
        List<Course> courses = new ArrayList<>();
        for (Long venueId : venueIds) {
            courses.addAll(courseMapper.selectByVenueId(venueId));
        }
        if (courses.isEmpty()) {
            return Result.success(PageResult.of(0L, Collections.emptyList()));
        }
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        Page<CourseOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CourseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CourseOrder::getCourseId, courseIds)
                .orderByDesc(CourseOrder::getCreateTime);
        Page<CourseOrder> pageResult = courseOrderMapper.selectPage(pageParam, wrapper);
        return Result.success(enrichOrders(pageResult));
    }

    private Map<String, Object> enrichOrders(Page<CourseOrder> pageResult) {
        List<Map<String, Object>> enrichedList = new ArrayList<>();
        for (CourseOrder order : pageResult.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", order.getId());
            item.put("orderNo", order.getOrderNo());
            item.put("userId", order.getUserId());
            item.put("courseId", order.getCourseId());
            item.put("status", order.getStatus());
            item.put("remark", order.getRemark());
            item.put("createTime", order.getCreateTime() != null ? order.getCreateTime().toString() : null);
            item.put("updateTime", order.getUpdateTime() != null ? order.getUpdateTime().toString() : null);

            // 关联课程信息
            Course course = courseMapper.selectById(order.getCourseId());
            if (course != null) {
                item.put("courseName", course.getName());
                item.put("courseType", course.getType());
                item.put("courseTime", (course.getStartTime() != null ? course.getStartTime().toString() : "") + " ~ " +
                        (course.getEndTime() != null ? course.getEndTime().toString() : ""));
                item.put("coursePrice", course.getPrice());
                // 关联场馆
                Venue venue = venueMapper.selectById(course.getVenueId());
                if (venue != null) {
                    item.put("venueName", venue.getName());
                }
                // 关联教练
                if (course.getCoachId() != null) {
                    Coach coach = coachMapper.selectById(course.getCoachId());
                    if (coach != null) {
                        item.put("coachName", coach.getName());
                    }
                }
            }

            // 关联用户信息
            SysUser user = sysUserMapper.selectById(order.getUserId());
            if (user != null) {
                item.put("userName", user.getNickname() != null ? user.getNickname() : user.getUsername());
            }

            enrichedList.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("records", enrichedList);
        return result;
    }

    @Override
    public Result getAllOrders(Integer page, Integer size) {
        Page<CourseOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CourseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CourseOrder::getCreateTime);
        Page<CourseOrder> pageResult = courseOrderMapper.selectPage(pageParam, wrapper);
        return Result.success(enrichOrders(pageResult));
    }

    @Override
    public Result updateOrderStatus(Long orderId, String status) {
        CourseOrder order = courseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 取消PAID订单时自动退款
        if ("CANCELLED".equals(status) && "PAID".equals(order.getStatus())) {
            refundToWallet(order.getUserId(), order);
            // 释放名额
            Course course = courseMapper.selectById(order.getCourseId());
            if (course != null && course.getEnrolledCount() > 0) {
                course.setEnrolledCount(course.getEnrolledCount() - 1);
                course.setUpdateTime(LocalDateTime.now());
                courseMapper.updateById(course);
            }
        }

        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        courseOrderMapper.updateById(order);

        if ("COMPLETED".equals(status)) {
            Course course = courseMapper.selectById(order.getCourseId());
            recordBehavior(order.getUserId(), "CHECK_IN", "COURSE",
                    order.getCourseId(), course != null ? course.getName() : null,
                    course != null ? course.getTags() : null);
        }

        return Result.success("状态更新成功");
    }

    @Override
    public Result cancelOrder(Long orderId, Long userId) {
        CourseOrder order = courseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (!"PAID".equals(order.getStatus()) && !"PENDING".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可取消");
        }
        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        courseOrderMapper.updateById(order);

        // 退款到钱包
        if ("PAID".equals(order.getStatus())) {
            refundToWallet(userId, order);
        }

        Course course = courseMapper.selectById(order.getCourseId());
        if (course != null && course.getEnrolledCount() > 0) {
            course.setEnrolledCount(course.getEnrolledCount() - 1);
            course.setUpdateTime(LocalDateTime.now());
            courseMapper.updateById(course);
        }

        return Result.success("取消成功，已退款到钱包");
    }

    /**
     * 用户申请退款 → status = REFUNDING
     */
    @Override
    public Result requestRefund(Long orderId, Long userId, String reason) {
        CourseOrder order = courseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (!"PAID".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可申请退款");
        }
        order.setStatus("REFUNDING");
        order.setRefundReason(reason);
        order.setUpdateTime(LocalDateTime.now());
        courseOrderMapper.updateById(order);
        log.info("User {} requested refund for order {}, reason={}", userId, orderId, reason);
        return Result.success("退款申请已提交，请等待管理员审核");
    }

    /**
     * 已过时间的已支付订单自动标记为 COMPLETED
     */
    private void completeExpiredOrders(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<CourseOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(CourseOrder::getUserId, userId)
                .eq(CourseOrder::getStatus, "PAID");
        List<CourseOrder> paidOrders = courseOrderMapper.selectList(orderWrapper);
        for (CourseOrder order : paidOrders) {
            if (order.getCourseId() != null && order.getOrderType() == null || "COURSE".equals(order.getOrderType())) {
                Course course = courseMapper.selectById(order.getCourseId());
                if (course != null && course.getEndTime() != null && course.getEndTime().isBefore(now)) {
                    order.setStatus("COMPLETED");
                    order.setUpdateTime(now);
                    courseOrderMapper.updateById(order);
                }
            }
        }
    }

    /**
     * 管理员审批通过退款 → status = REFUNDED，退款到钱包
     */
    @Override
    public Result approveRefund(Long orderId) {
        CourseOrder order = courseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"REFUNDING".equals(order.getStatus())) {
            throw new BusinessException("该订单不在退款审核中");
        }
        refundToWallet(order.getUserId(), order);
        order.setStatus("REFUNDED");
        order.setUpdateTime(LocalDateTime.now());
        courseOrderMapper.updateById(order);

        // 释放名额
        Course course = courseMapper.selectById(order.getCourseId());
        if (course != null && course.getEnrolledCount() > 0) {
            course.setEnrolledCount(course.getEnrolledCount() - 1);
            course.setUpdateTime(LocalDateTime.now());
            courseMapper.updateById(course);
        }
        log.info("Admin approved refund for order {}", orderId);
        return Result.success("退款成功，金额已退回用户钱包");
    }

    /**
     * 管理员拒绝退款 → status = PAID（恢复原状态）
     */
    @Override
    public Result rejectRefund(Long orderId) {
        CourseOrder order = courseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"REFUNDING".equals(order.getStatus())) {
            throw new BusinessException("该订单不在退款审核中");
        }
        order.setStatus("PAID");
        order.setUpdateTime(LocalDateTime.now());
        courseOrderMapper.updateById(order);
        log.info("Admin rejected refund for order {}", orderId);
        return Result.success("已拒绝退款申请");
    }

    private void refundToWallet(Long userId, CourseOrder order) {
        Course course = courseMapper.selectById(order.getCourseId());
        java.math.BigDecimal refundAmount = course != null && course.getPrice() != null
                ? course.getPrice() : java.math.BigDecimal.ZERO;

        Wallet wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(java.math.BigDecimal.ZERO);
            wallet.setCreateTime(LocalDateTime.now());
            wallet.setUpdateTime(LocalDateTime.now());
            walletMapper.insert(wallet);
        }

        java.math.BigDecimal before = wallet.getBalance();
        java.math.BigDecimal after = before.add(refundAmount);
        wallet.setBalance(after);
        wallet.setUpdateTime(LocalDateTime.now());
        walletMapper.updateById(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setUserId(userId);
        tx.setType("REFUND");
        tx.setAmount(refundAmount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setOrderId(order.getId());
        tx.setRemark("退款：" + (course != null ? course.getName() : "课程退款"));
        tx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(tx);

        log.info("Refunded {} to user {} for order {}", refundAmount, userId, order.getId());
    }

    private void recordBehavior(Long userId, String behaviorType, String targetType,
                                 Long targetId, String targetName, String targetTags) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setBehaviorType(behaviorType);
        behavior.setTargetType(targetType);
        behavior.setTargetId(targetId);
        behavior.setTargetName(targetName);
        behavior.setTargetTags(targetTags);
        behavior.setCreateTime(LocalDateTime.now());
        userBehaviorMapper.insert(behavior);
    }
}
