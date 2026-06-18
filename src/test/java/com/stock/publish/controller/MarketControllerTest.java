package com.stock.publish.controller;

import com.stock.publish.calculation.KLineAggregator;
import com.stock.publish.calculation.TopTraderEngine;
import com.stock.publish.interceptor.UserContext;
import com.stock.publish.service.MarketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class MarketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MarketService marketService;

    @Mock
    private KLineAggregator kLineAggregator;

    @Mock
    private TopTraderEngine topTraderEngine;

    @InjectMocks
    private MarketController marketController;

    // 用于 Mock 静态类 UserContext
    private MockedStatic<UserContext> mockedUserContext;

    @BeforeEach
    void setUp() {
        // 配置MockMvc，因为现在返回的是 JSON 格式的 ApiResponse，需要配置 JSON 转换器
        mockMvc = MockMvcBuilders.standaloneSetup(marketController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        // 开启对 UserContext 静态方法的 Mock
        mockedUserContext = Mockito.mockStatic(UserContext.class);
    }

    @AfterEach
    void tearDown() {
        // 用完静态 Mock 必须关闭，否则会污染/影响其他测试类
        if (mockedUserContext != null) {
            mockedUserContext.close();
        }
    }

    @Test
    void testKline_GuestAccess_ReturnsFail() throws Exception {
        // 模拟当前线程上下文中的角色为 GUEST
        mockedUserContext.when(UserContext::getRole).thenReturn(UserContext.UserRole.GUEST);

        mockMvc.perform(get("/market/kline")
                        .param("stockCode", "600519")
                        .param("period", "1D"))
                .andExpect(status().isOk()) // HTTP 状态码现在是 200 OK
                .andExpect(jsonPath("$.code").value(403)) // 业务的 code 是 403
                .andExpect(jsonPath("$.message").value("游客无权查看K线图"));
    }

    @Test
    void testKline_StandardAccess_1H_ReturnsFail() throws Exception {
        // 模拟当前线程上下文中的角色为 STANDARD
        mockedUserContext.when(UserContext::getRole).thenReturn(UserContext.UserRole.STANDARD);

        mockMvc.perform(get("/market/kline")
                        .param("stockCode", "600519")
                        .param("period", "1H"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("普通用户仅可查看日K线(1D)"));
    }

    @Test
    void testKline_VipAccess_Success() throws Exception {
        // 模拟当前线程上下文中的角色为 PREMIUM_VIP
        mockedUserContext.when(UserContext::getRole).thenReturn(UserContext.UserRole.PREMIUM_VIP);
        
        when(kLineAggregator.getKLineData(anyString(), anyString(), any(), any()))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/market/kline")
                        .param("stockCode", "600519")
                        .param("period", "5M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200)) // 断言封装类 ApiResponse.ok() 的返回值
                .andExpect(jsonPath("$.message").value("success"));
    }
}