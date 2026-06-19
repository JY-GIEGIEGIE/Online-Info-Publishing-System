package com.stock.publish.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stock.publish.entity.LocalUserSubscription;
import com.stock.publish.mapper.LocalUserSubscriptionMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final LocalUserSubscriptionMapper subscriptionMapper;

    public AuthInterceptor(LocalUserSubscriptionMapper subscriptionMapper) {
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // 清理当前线程的上下文
        UserContext.clear();

        // 从 Header 提取 "Authorization: Bearer {token}"
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            UserContext.setRole(UserContext.UserRole.GUEST);
            return true;
        }

        String token = authHeader.substring(7);

        // 外部鉴权接口调用（Mock 模拟账户系统 clientLoginAuth）
        // 后续替换为：HTTP POST http://account-system/api/v1/auth/clientLoginAuth
        // 请求体：{ fund_acc_no, trade_password }
        // 返回：{ code, fund_acc_no, sec_acc_no, status }
        // sec_acc_no 不为空 → 证书已绑定；status == 0 → 账户正常
        boolean authSuccess = true;
        String fundAccNo = "F0001";   // 对应 local_user_subscription.global_user_id
        String secAccNo = "S0001";    // 不为空 = 安全证书已绑定

        if ("invalid_token".equals(token)) {
            authSuccess = false;
        }
        if ("no_cert_token".equals(token)) {
            secAccNo = null; // 模拟证书未绑定
        }
        if ("vip_token".equals(token)) {
            fundAccNo = "F0002"; // Mock VIP 用户
        }

        // 登录失败 或 证书未绑定 → 降级 GUEST
        if (!authSuccess || secAccNo == null || secAccNo.isEmpty()) {
            UserContext.setRole(UserContext.UserRole.GUEST);
            return true;
        }

        // 校验通过 → 查 local_user_subscription
        LambdaQueryWrapper<LocalUserSubscription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LocalUserSubscription::getGlobalUserId, fundAccNo);
        LocalUserSubscription subscription = subscriptionMapper.selectOne(queryWrapper);

        // 首次访问自动注册
        if (subscription == null) {
            subscription = new LocalUserSubscription();
            subscription.setGlobalUserId(fundAccNo);
            subscription.setIsPremium(false);
            subscriptionMapper.insert(subscription);
        }

        if (Boolean.TRUE.equals(subscription.getIsPremium())) {
            UserContext.setRole(UserContext.UserRole.PREMIUM_VIP);
        } else {
            UserContext.setRole(UserContext.UserRole.STANDARD);
        }

        UserContext.setGlobalUserId(fundAccNo);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
