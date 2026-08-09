package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.OrderRequest;
import com.fitness.entity.SysUser;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Resource
    private SysUserMapper sysUserMapper;

    @PostMapping("/create")
    public Result create(@RequestBody @Valid OrderRequest request) {
        Long userId = getCurrentUserId();
        log.info("Create order: userId={}, courseId={}", userId, request.getCourseId());
        return orderService.createOrder(userId, request);
    }

    @GetMapping("/my")
    public Result my(@RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        log.info("Get my orders: userId={}, page={}, size={}", userId, page, size);
        return orderService.getMyOrders(userId, page, size);
    }

    @GetMapping("/venue")
    public Result venue(@RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        // 管理员查看全部订单，场馆管理员只看自己的场馆订单
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null && "ADMIN".equals(user.getRole())) {
            log.info("Admin get all orders: page={}, size={}", page, size);
            return orderService.getAllOrders(page, size);
        }
        log.info("Get venue orders: adminUserId={}, page={}, size={}", userId, page, size);
        return orderService.getVenueOrders(userId, page, size);
    }

    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Long id,
                               @RequestParam String status) {
        log.info("Update order status: orderId={}, status={}", id, status);
        return orderService.updateOrderStatus(id, status);
    }

    @PutMapping("/{id}/cancel")
    public Result cancel(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("Cancel order: userId={}, orderId={}", userId, id);
        return orderService.cancelOrder(id, userId);
    }

    @PutMapping("/{id}/request-refund")
    public Result requestRefund(@PathVariable Long id, @RequestParam(required = false) String reason) {
        Long userId = getCurrentUserId();
        log.info("Request refund: userId={}, orderId={}, reason={}", userId, id, reason);
        return orderService.requestRefund(id, userId, reason);
    }

    @PutMapping("/{id}/approve-refund")
    public Result approveRefund(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("Approve refund: adminUserId={}, orderId={}", userId, id);
        return orderService.approveRefund(id);
    }

    @PutMapping("/{id}/reject-refund")
    public Result rejectRefund(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("Reject refund: adminUserId={}, orderId={}", userId, id);
        return orderService.rejectRefund(id);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(401, "请先登录");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String) {
            String str = (String) principal;
            try {
                return Long.valueOf(str);
            } catch (NumberFormatException e) {
                var user = sysUserMapper.selectByUsername(str);
                if (user != null) {
                    return user.getId();
                }
            }
        }
        throw new BusinessException(401, "无法获取用户信息");
    }
}
