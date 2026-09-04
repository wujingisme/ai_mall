package com.aimall.auth.mapper;

import com.aimall.auth.entity.MallUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 商城用户表的数据访问接口。 */
public interface MallUserMapper extends BaseMapper<MallUser> {
}
