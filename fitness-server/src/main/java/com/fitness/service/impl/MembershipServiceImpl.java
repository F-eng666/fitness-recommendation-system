package com.fitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.entity.MembershipCard;
import com.fitness.entity.UserMembership;
import com.fitness.entity.Venue;
import com.fitness.entity.Wallet;
import com.fitness.entity.WalletTransaction;
import com.fitness.mapper.MembershipCardMapper;
import com.fitness.mapper.UserMembershipMapper;
import com.fitness.mapper.VenueMapper;
import com.fitness.mapper.WalletMapper;
import com.fitness.mapper.WalletTransactionMapper;
import com.fitness.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipServiceImpl implements MembershipService {

    private final MembershipCardMapper cardMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final VenueMapper venueMapper;

    @Override
    public Result getCardsByVenue(Long venueId) {
        List<MembershipCard> cards = cardMapper.selectByVenueId(venueId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (MembershipCard card : cards) {
            list.add(cardToMap(card));
        }
        return Result.success(list);
    }

    @Override
    public Result getHotCards() {
        // Fetch all approved venues
        QueryWrapper<Venue> venueWrapper = new QueryWrapper<>();
        venueWrapper.eq("status", "APPROVED");
        List<Venue> allVenues = venueMapper.selectList(venueWrapper);

        // Fetch all active cards, index by venueId + cardType
        List<MembershipCard> allCards = cardMapper.selectAllActive();
        Map<String, MembershipCard> cardIndex = new HashMap<>();
        for (MembershipCard card : allCards) {
            cardIndex.put(card.getVenueId() + "_" + card.getCardType(), card);
        }

        // Group by card type
        String[] typeOrder = {"SESSION", "WEEKLY", "MONTHLY", "QUARTERLY"};
        String[] typeNames = {"次卡", "周卡", "月卡", "季卡"};
        List<Map<String, Object>> list = new ArrayList<>();

        for (int i = 0; i < typeOrder.length; i++) {
            String type = typeOrder[i];
            List<Map<String, Object>> venues = new ArrayList<>();
            int minPrice = Integer.MAX_VALUE;

            for (Venue venue : allVenues) {
                String key = venue.getId() + "_" + type;
                MembershipCard card = cardIndex.get(key);

                Map<String, Object> v = new HashMap<>();
                v.put("venueId", venue.getId());
                v.put("venueName", venue.getName());

                if (card != null) {
                    v.put("cardId", card.getId());
                    v.put("price", card.getPrice());
                    v.put("description", card.getDescription());
                    v.put("durationDays", card.getDurationDays());
                    v.put("sessionCount", card.getSessionCount());
                    v.put("available", true);
                    if (card.getPrice().intValue() < minPrice) {
                        minPrice = card.getPrice().intValue();
                    }
                } else {
                    v.put("cardId", null);
                    v.put("price", 0);
                    v.put("description", "该场馆暂未开通此卡型");
                    v.put("durationDays", null);
                    v.put("sessionCount", null);
                    v.put("available", false);
                }
                venues.add(v);
            }

            Map<String, Object> group = new LinkedHashMap<>();
            group.put("cardType", type);
            group.put("typeName", typeNames[i]);
            group.put("minPrice", minPrice == Integer.MAX_VALUE ? 0 : minPrice);
            group.put("venues", venues);
            list.add(group);
        }

        return Result.success(list);
    }

    @Override
    @Transactional
    public Result purchaseCard(Long cardId, Long venueId, Long userId) {
        // 1. Validate card
        MembershipCard card = cardMapper.selectById(cardId);
        if (card == null || card.getStatus() == 0) {
            throw new BusinessException("会员卡不存在或已下架");
        }

        // 2. Get user wallet
        Wallet wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setCreateTime(LocalDateTime.now());
            wallet.setUpdateTime(LocalDateTime.now());
            walletMapper.insert(wallet);
        }

        // 3. Check balance
        if (wallet.getBalance().compareTo(card.getPrice()) < 0) {
            throw new BusinessException("余额不足，请先充值");
        }

        // 4. Generate order number
        String orderNo = "MC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));

        // 5. Deduct wallet
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.subtract(card.getPrice());
        wallet.setBalance(after);
        wallet.setUpdateTime(LocalDateTime.now());
        walletMapper.updateById(wallet);

        // 6. Create wallet transaction
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setUserId(userId);
        tx.setType("PURCHASE_CARD");
        tx.setAmount(card.getPrice().negate());
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setRemark("购买会员卡: " + card.getName());
        tx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(tx);

        // 7. Create membership record
        UserMembership membership = new UserMembership();
        membership.setUserId(userId);
        membership.setCardId(card.getId());
        membership.setVenueId(card.getVenueId());
        membership.setCardType(card.getCardType());
        membership.setOrderNo(orderNo);
        membership.setPrice(card.getPrice());
        // Set remaining sessions for SESSION type
        if (card.getSessionCount() != null) {
            membership.setRemainingSessions(card.getSessionCount());
        }
        // Set expire time for time-based cards
        if (card.getDurationDays() != null) {
            membership.setExpireTime(LocalDateTime.now().plusDays(card.getDurationDays()));
        }
        membership.setStatus("ACTIVE");
        membership.setCreateTime(LocalDateTime.now());
        userMembershipMapper.insert(membership);

        log.info("User {} purchased card {} ({}), order: {}, balance: {} -> {}",
                userId, cardId, card.getName(), orderNo, before, after);

        // 8. Return result
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("cardName", card.getName());
        result.put("cardType", card.getCardType());
        result.put("price", card.getPrice());
        result.put("balance", after);
        result.put("venueId", card.getVenueId());
        if (membership.getExpireTime() != null) {
            result.put("expireTime", membership.getExpireTime().toString());
        }
        if (membership.getRemainingSessions() != null) {
            result.put("remainingSessions", membership.getRemainingSessions());
        }
        return Result.success(result);
    }

    @Override
    public Result getMyCards(Long userId, Integer page, Integer size) {
        Page<UserMembership> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserMembership> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMembership::getUserId, userId)
                .orderByDesc(UserMembership::getCreateTime);
        Page<UserMembership> pageResult = userMembershipMapper.selectPage(pageParam, wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (UserMembership m : pageResult.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("cardId", m.getCardId());
            map.put("venueId", m.getVenueId());
            map.put("cardType", m.getCardType());
            map.put("orderNo", m.getOrderNo());
            map.put("price", m.getPrice());
            map.put("remainingSessions", m.getRemainingSessions());
            map.put("expireTime", m.getExpireTime() != null ? m.getExpireTime().toString() : null);
            map.put("status", m.getStatus());
            map.put("createTime", m.getCreateTime() != null ? m.getCreateTime().toString() : null);
            // Fetch card name
            MembershipCard card = cardMapper.selectById(m.getCardId());
            map.put("cardName", card != null ? card.getName() : "未知");
            // Fetch venue name
            Venue venue = venueMapper.selectById(m.getVenueId());
            map.put("venueName", venue != null ? venue.getName() : "未知场馆");
            list.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", pageResult.getTotal());
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return Result.success(result);
    }

    @Override
    @Transactional
    public Result saveVenueCards(Long venueId, List<Map<String, Object>> cards) {
        if (cards == null || cards.isEmpty()) {
            return Result.success("ok");
        }

        for (Map<String, Object> cardData : cards) {
            String cardType = (String) cardData.get("cardType");
            Object priceObj = cardData.get("price");
            if (cardType == null) continue;

            // Find existing card for this venue+type
            LambdaQueryWrapper<MembershipCard> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MembershipCard::getVenueId, venueId)
                   .eq(MembershipCard::getCardType, cardType);
            MembershipCard existing = cardMapper.selectOne(wrapper);

            String[] typeNames = {"次卡", "周卡", "月卡", "季卡"};
            String[] typeOrder = {"SESSION", "WEEKLY", "MONTHLY", "QUARTERLY"};
            String typeName = cardType;
            for (int i = 0; i < typeOrder.length; i++) {
                if (typeOrder[i].equals(cardType)) { typeName = typeNames[i]; break; }
            }

            if (priceObj == null || new java.math.BigDecimal(priceObj.toString()).compareTo(java.math.BigDecimal.ZERO) <= 0) {
                // Price is 0 or null — deactivate/delete existing card
                if (existing != null) {
                    existing.setStatus(0);
                    cardMapper.updateById(existing);
                }
                continue;
            }

            java.math.BigDecimal price = new java.math.BigDecimal(priceObj.toString());
            String name = (String) cardData.getOrDefault("name", "");
            if (name.isEmpty()) {
                Venue venue = venueMapper.selectById(venueId);
                name = (venue != null ? venue.getName() : "场馆") + "·" + typeName;
            }

            if (existing != null) {
                existing.setPrice(price);
                existing.setName(name);
                existing.setStatus(1);
                existing.setUpdateTime(java.time.LocalDateTime.now());
                cardMapper.updateById(existing);
            } else {
                MembershipCard card = new MembershipCard();
                card.setVenueId(venueId);
                card.setCardType(cardType);
                card.setName(name);
                card.setPrice(price);
                card.setStatus(1);
                card.setCreateTime(java.time.LocalDateTime.now());
                card.setUpdateTime(java.time.LocalDateTime.now());

                switch (cardType) {
                    case "SESSION":
                        card.setSessionCount(1);
                        card.setDescription("单次课程体验");
                        break;
                    case "WEEKLY":
                        card.setDurationDays(7);
                        card.setDescription("7天无限次使用");
                        break;
                    case "MONTHLY":
                        card.setDurationDays(30);
                        card.setDescription("30天无限次使用");
                        break;
                    case "QUARTERLY":
                        card.setDurationDays(90);
                        card.setDescription("90天无限次使用");
                        break;
                }
                cardMapper.insert(card);
            }
        }

        log.info("Saved {} card(s) for venue {}", cards.size(), venueId);
        return Result.success("ok");
    }

    private Map<String, Object> cardToMap(MembershipCard card) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", card.getId());
        map.put("venueId", card.getVenueId());
        map.put("name", card.getName());
        map.put("cardType", card.getCardType());
        map.put("price", card.getPrice());
        map.put("durationDays", card.getDurationDays());
        map.put("sessionCount", card.getSessionCount());
        map.put("description", card.getDescription());
        map.put("status", card.getStatus());
        return map;
    }
}
