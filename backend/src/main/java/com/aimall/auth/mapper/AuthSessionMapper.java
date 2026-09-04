package com.aimall.auth.mapper;

import com.aimall.auth.entity.AuthSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 刷新令牌会话的数据访问接口；数据库保存的是令牌摘要而非明文。 */
public interface AuthSessionMapper extends BaseMapper<AuthSession> {
}
