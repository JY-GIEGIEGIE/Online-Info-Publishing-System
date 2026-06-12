package com.stock.publish.service.impl;

import com.stock.publish.mapper.LocalUserSubscriptionMapper;
import com.stock.publish.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final LocalUserSubscriptionMapper subscriptionMapper;

    public UserServiceImpl(LocalUserSubscriptionMapper subscriptionMapper) {
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    public void upgradeToVip(String globalUserId) {
        // TODO: 查询用户订阅记录，更新 is_premium = true, upgrade_time = now()
        throw new UnsupportedOperationException("TODO");
    }
}
