package com.fitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.dto.BookingRequest;
import com.fitness.entity.*;
import com.fitness.mapper.*;
import com.fitness.service.PersonalTrainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalTrainingServiceImpl implements PersonalTrainingService {

    private final PersonalTrainingMapper ptMapper;
    private final CourseMapper courseMapper;
    private final VenueMapper venueMapper;
    private final CoachMapper coachMapper;
    private final CourseOrderMapper courseOrderMapper;
    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    private static final BigDecimal PT_HOURLY_RATE = new BigDecimal("200");

    @Override
    public Result getAvailableSlots(Long coachId, Long venueId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            throw new BusinessException("场馆不存在");
        }

        String businessHours = venue.getBusinessHours();
        LocalTime openTime = LocalTime.of(8, 0);
        LocalTime closeTime = LocalTime.of(22, 0);
        if (businessHours != null && businessHours.contains("-")) {
            String[] parts = businessHours.split("-");
            try {
                openTime = LocalTime.parse(parts[0].trim());
                closeTime = LocalTime.parse(parts[1].trim());
            } catch (Exception e) { /* use defaults */ }
        }

        List<LocalTime[]> occupied = getOccupiedSlots(coachId, date);

        List<Map<String, String>> slots = new ArrayList<>();
        LocalTime slot = openTime;
        while (slot.isBefore(closeTime.minusMinutes(30))) {
            LocalTime slotEnd = slot.plusMinutes(60);
            if (slotEnd.isAfter(closeTime)) break;

            boolean available = true;
            for (LocalTime[] occ : occupied) {
                if (slot.isBefore(occ[1]) && slotEnd.isAfter(occ[0])) {
                    available = false;
                    break;
                }
            }

            if (date.isAfter(LocalDate.now())
                || (date.equals(LocalDate.now()) && !slot.isBefore(LocalTime.now().plusMinutes(30)))) {
                Map<String, String> map = new HashMap<>();
                map.put("start", slot.toString());
                map.put("end", slotEnd.toString());
                map.put("label", slot + " - " + slotEnd);
                slots.add(map);
            }

            slot = slot.plusMinutes(30);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", dateStr);
        result.put("openTime", openTime.toString());
        result.put("closeTime", closeTime.toString());
        result.put("slots", slots);
        return Result.success(result);
    }

    private List<LocalTime[]> getOccupiedSlots(Long coachId, LocalDate date) {
        List<LocalTime[]> occupied = new ArrayList<>();

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.eq(Course::getCoachId, coachId)
                .ge(Course::getStartTime, dayStart)
                .lt(Course::getStartTime, dayEnd);
        List<Course> courses = courseMapper.selectList(courseWrapper);
        for (Course c : courses) {
            occupied.add(new LocalTime[]{c.getStartTime().toLocalTime(), c.getEndTime().toLocalTime()});
        }

        LambdaQueryWrapper<PersonalTraining> ptWrapper = new LambdaQueryWrapper<>();
        ptWrapper.eq(PersonalTraining::getCoachId, coachId)
                .ge(PersonalTraining::getStartTime, dayStart)
                .lt(PersonalTraining::getStartTime, dayEnd)
                .in(PersonalTraining::getStatus, "CONFIRMED", "PAID");
        List<PersonalTraining> bookings = ptMapper.selectList(ptWrapper);
        for (PersonalTraining pt : bookings) {
            occupied.add(new LocalTime[]{pt.getStartTime().toLocalTime(), pt.getEndTime().toLocalTime()});
        }

        return occupied;
    }

    @Override
    public Result book(Long userId, BookingRequest request) {
        Venue venue = venueMapper.selectById(request.getVenueId());
        if (venue == null) throw new BusinessException("场馆不存在");

        Coach coach = coachMapper.selectById(request.getCoachId());
        if (coach == null) throw new BusinessException("教练不存在");

        if (request.getStartTime().isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new BusinessException("预约时间必须晚于当前时间30分钟");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }

        // 校验营业时间
        LocalTime start = request.getStartTime().toLocalTime();
        LocalTime end = request.getEndTime().toLocalTime();
        String businessHours = venue.getBusinessHours();
        if (businessHours != null && businessHours.contains("-")) {
            String[] parts = businessHours.split("-");
            LocalTime openTime = LocalTime.parse(parts[0].trim());
            LocalTime closeTime = LocalTime.parse(parts[1].trim());
            if (start.isBefore(openTime) || end.isAfter(closeTime)) {
                throw new BusinessException("预约时间必须在场馆营业时间内(" + businessHours + ")");
            }
        }

        // 校验时间冲突
        List<LocalTime[]> occupied = getOccupiedSlots(request.getCoachId(), request.getStartTime().toLocalDate());
        for (LocalTime[] occ : occupied) {
            if (start.isBefore(occ[1]) && end.isAfter(occ[0])) {
                throw new BusinessException("该时段教练已有安排，请选择其他时间");
            }
        }

        // 计算价格（按小时比例）
        long minutes = ChronoUnit.MINUTES.between(request.getStartTime(), request.getEndTime());
        BigDecimal price = PT_HOURLY_RATE.multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        // 检查钱包余额
        Wallet wallet = walletMapper.selectByUserId(userId);
        BigDecimal balance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(price) < 0) {
            throw new BusinessException("余额不足，需要 ¥" + price + "，当前余额 ¥" + balance + "，请先充值");
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
        tx.setRemark("私教预约：" + coach.getName() + " " + request.getStartTime().toLocalDate());
        tx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(tx);

        // 创建私教预约记录
        PersonalTraining pt = new PersonalTraining();
        pt.setUserId(userId);
        pt.setCoachId(request.getCoachId());
        pt.setVenueId(request.getVenueId());
        pt.setStartTime(request.getStartTime());
        pt.setEndTime(request.getEndTime());
        pt.setStatus("PAID");
        pt.setCreateTime(LocalDateTime.now());
        pt.setUpdateTime(LocalDateTime.now());
        ptMapper.insert(pt);

        // 创建订单
        CourseOrder order = new CourseOrder();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setUserId(userId);
        order.setOrderType("PT");
        order.setPtId(pt.getId());
        order.setAmount(price);
        order.setStatus("PAID");
        order.setRemark("私教预约：" + coach.getName());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        courseOrderMapper.insert(order);

        log.info("User {} booked PT with coach {} at {}, price={}, order={}", userId, request.getCoachId(), request.getStartTime(), price, order.getId());

        Map<String, Object> resp = new HashMap<>();
        resp.put("ptId", pt.getId());
        resp.put("orderId", order.getId());
        resp.put("price", price);
        resp.put("balanceAfter", wallet.getBalance());
        resp.put("coachName", coach.getName());
        return Result.success(resp);
    }

    @Override
    public Result getMyBookings(Long userId) {
        LambdaQueryWrapper<PersonalTraining> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PersonalTraining::getUserId, userId)
                .orderByDesc(PersonalTraining::getStartTime);
        List<PersonalTraining> list = ptMapper.selectList(wrapper);

        List<Map<String, Object>> result = list.stream().map(pt -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", pt.getId());
            m.put("coachId", pt.getCoachId());
            m.put("venueId", pt.getVenueId());
            m.put("startTime", pt.getStartTime() != null ? pt.getStartTime().toString() : null);
            m.put("endTime", pt.getEndTime() != null ? pt.getEndTime().toString() : null);
            m.put("status", pt.getStatus());

            Coach coach = coachMapper.selectById(pt.getCoachId());
            if (coach != null) m.put("coachName", coach.getName());
            Venue venue = venueMapper.selectById(pt.getVenueId());
            if (venue != null) m.put("venueName", venue.getName());

            m.put("createTime", pt.getCreateTime() != null ? pt.getCreateTime().toString() : null);
            return m;
        }).collect(Collectors.toList());

        return Result.success(result);
    }
}
