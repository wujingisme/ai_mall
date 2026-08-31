package com.aimall.auth.service;

import com.aimall.auth.entity.MallUser;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.auth.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminCustomerService {
    private final MallUserMapper mapper;
    public AdminCustomerService(MallUserMapper mapper) { this.mapper = mapper; }

    public CustomerPageResponse list(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<MallUser> query = new LambdaQueryWrapper<MallUser>()
                .eq(MallUser::getEnabled, true)
                .apply("FIND_IN_SET('CUSTOMER', roles) > 0");
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(group -> group.like(MallUser::getUsername, value).or().like(MallUser::getDisplayName, value));
        }
        query.orderByDesc(MallUser::getId);
        Page<MallUser> result = mapper.selectPage(Page.of(page, pageSize), query);
        return new CustomerPageResponse(result.getRecords().stream()
                .map(user -> new CustomerSummaryResponse(user.getId().toString(), user.getUsername(),
                        user.getDisplayName(), user.getAvatarUrl())).toList(),
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }
}
