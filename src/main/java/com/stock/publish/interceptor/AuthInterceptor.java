package com.stock.publish.interceptor;

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
        // TODO: 1. 从 Header 提取 "Authorization: Bearer {token}"
        // TODO: 2. 无 Token → UserContext.setRole(GUEST)
        // TODO: 3. 有 Token → HTTP POST 调用外部鉴权接口 http://account-system/api/v1/auth/certificate-validate
        // TODO: 4. Mock 返回 {"global_user_id":"U1001","certificate_bind":true}
        // TODO: 5. certificate_bind==false 或请求失败 → 降级 GUEST
        // TODO: 6. 校验通过 → 查 local_user_subscription，无记录则插入
        // TODO: 7. 根据 is_premium 设置 STANDARD 或 PREMIUM_VIP
        // TODO: 8. 将 global_user_id 和 role 存入 UserContext (ThreadLocal)
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
