package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {

    @Select("SELECT * FROM wallet_transaction WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<WalletTransaction> selectByUserId(@Param("userId") Long userId);
}
