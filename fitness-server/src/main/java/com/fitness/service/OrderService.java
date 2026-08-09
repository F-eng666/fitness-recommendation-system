package com.fitness.service;

import com.fitness.common.Result;
import com.fitness.dto.OrderRequest;

public interface OrderService {
    Result createOrder(Long userId, OrderRequest request);
    Result getMyOrders(Long userId, Integer page, Integer size);
    Result getVenueOrders(Long adminUserId, Integer page, Integer size);
    Result getAllOrders(Integer page, Integer size);
    Result updateOrderStatus(Long orderId, String status);
    Result cancelOrder(Long orderId, Long userId);
    Result requestRefund(Long orderId, Long userId, String reason);
    Result approveRefund(Long orderId);
    Result rejectRefund(Long orderId);
}