package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE phone = #{phone}")
    SysUser selectByPhone(String phone);
}
