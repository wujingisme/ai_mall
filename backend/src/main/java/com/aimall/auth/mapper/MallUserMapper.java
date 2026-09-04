package com.aimall.auth.mapper;

import com.aimall.auth.entity.MallUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
/**
 * 商城用户表的数据访问接口。
 *
 * <p>角色目前仍保存在同一列的逗号分隔字符串中，因此涉及角色追加时不能把整行
 * 读到 Java 后无条件覆盖：必须先锁定用户行，再只更新 roles 字段，避免同时登录或
 * 管理操作把用户的其他资料覆盖掉。</p>
 */
public interface MallUserMapper extends BaseMapper<MallUser> {
    /** 给当前已登录后台账号加上 CUSTOMER 角色前锁行，保证并发点击使用同一份角色数据。 */
    @Select("SELECT * FROM mall_user WHERE id = #{userId} FOR UPDATE")
    MallUser selectForUpdateById(@Param("userId") Long userId);

    /** 只追加角色并更新时间；调用方必须在事务中先通过 selectForUpdateById 获取行锁。 */
    @Update("UPDATE mall_user SET roles = #{roles}, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{userId}")
    int updateRoles(@Param("userId") Long userId, @Param("roles") String roles);
}
