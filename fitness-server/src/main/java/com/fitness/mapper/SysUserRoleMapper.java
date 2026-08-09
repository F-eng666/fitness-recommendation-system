package com.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.entity.SysRole;
import com.fitness.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select("SELECT r.* FROM sys_role r INNER JOIN sys_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<SysRole> selectRolesByUserId(Long userId);
}
