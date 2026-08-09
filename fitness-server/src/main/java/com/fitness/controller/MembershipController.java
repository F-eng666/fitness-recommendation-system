package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/cards")
    public Result getCards(@RequestParam(required = false) Long venueId) {
        if (venueId != null) {
            return membershipService.getCardsByVenue(venueId);
        }
        return membershipService.getHotCards();
    }

    @GetMapping("/cards/hot")
    public Result getHotCards() {
        return membershipService.getHotCards();
    }

    @PostMapping("/purchase")
    public Result purchase(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        Long cardId = body.get("cardId") != null ? Long.valueOf(body.get("cardId").toString()) : null;
        Long venueId = body.get("venueId") != null ? Long.valueOf(body.get("venueId").toString()) : null;
        if (cardId == null) {
            throw new BusinessException("请选择要购买的会员卡");
        }
        log.info("Purchase card: userId={}, cardId={}, venueId={}", userId, cardId, venueId);
        return membershipService.purchaseCard(cardId, venueId, userId);
    }

    @GetMapping("/my")
    public Result myCards(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        log.info("Get my memberships: userId={}, page={}, size={}", userId, page, size);
        return membershipService.getMyCards(userId, page, size);
    }

    @PostMapping("/admin/venue-cards")
    public Result saveVenueCards(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        Long venueId = body.get("venueId") != null ? Long.valueOf(body.get("venueId").toString()) : null;
        if (venueId == null) {
            throw new BusinessException("场馆ID不能为空");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) body.get("cards");
        log.info("Save venue cards: adminUserId={}, venueId={}", userId, venueId);
        return membershipService.saveVenueCards(venueId, cards);
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
