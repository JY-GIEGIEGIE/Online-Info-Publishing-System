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
            // 没有对应Token，设置角色为GUEST
            UserContext.setRole(UserContext.UserRole.GUEST);
            return true;
        }

        String token = authHeader.substring(7);

        // 外部鉴权接口调用（ Mock 模拟）
        // 后续阶段，此处需整体替换为 RestTemplate 发起真实的 HTTP POST 请求
        // 目标接口契约：http://account-system/api/v1/auth/certificate-validate
        boolean isSuccess = true;
        boolean certificateBind = true;
        String globalUserId = "U1001"; // Mock 的默认返回ID

        // 简单的 Mock 逻辑，用于模拟异常 Token
        if ("invalid_token".equals(token)) {
            isSuccess = false;
        }
        if ("no_cert_token".equals(token)) {
            certificateBind = false;
        }

        // 4. certificate_bind==false 或请求失败 → 降级 GUEST (不返回 401 拦截)
        if (!isSuccess || !certificateBind) {
            UserContext.setRole(UserContext.UserRole.GUEST);
            return true;
        }

        // 5. 校验通过 → 查 local_user_subscription
        LambdaQueryWrapper<LocalUserSubscription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LocalUserSubscription::getGlobalUserId, globalUserId);
        LocalUserSubscription subscription = subscriptionMapper.selectOne(queryWrapper);

        // 无记录则自动插入
        if (subscription == null) {
            subscription = new LocalUserSubscription();
            subscription.setGlobalUserId(globalUserId);
            subscription.setIsPremium(false);
            subscriptionMapper.insert(subscription);
        }

        // 根据 is_premium 设置用户类别为 STANDARD 或 PREMIUM_VIP
        if (Boolean.TRUE.equals(subscription.getIsPremium())) {
            UserContext.setRole(UserContext.UserRole.PREMIUM_VIP);
        } else {
            UserContext.setRole(UserContext.UserRole.STANDARD);
        }

        // 将 global_user_id 和 role 存入 UserContext
        UserContext.setGlobalUserId(globalUserId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
