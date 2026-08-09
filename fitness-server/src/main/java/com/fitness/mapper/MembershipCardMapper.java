package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.MembershipCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MembershipCardMapper extends BaseMapper<MembershipCard> {

    @Select("SELECT * FROM membership_card WHERE venue_id = #{venueId} AND status = 1 ORDER BY FIELD(card_type, 'SESSION','WEEKLY','MONTHLY','QUARTERLY')")
    List<MembershipCard> selectByVenueId(@Param("venueId") Long venueId);

    @Select("SELECT * FROM membership_card WHERE status = 1 ORDER BY venue_id, FIELD(card_type, 'SESSION','WEEKLY','MONTHLY','QUARTERLY')")
    List<MembershipCard> selectAllActive();
}
