package com.stock.publish.controller;

import com.stock.publish.calculation.KLineAggregator;
import com.stock.publish.dto.QuoteDTO;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MarketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MarketService marketService;

    @Mock
    private KLineAggregator kLineAggregator;

    @InjectMocks
    private MarketController marketController;

    private MockedStatic<UserContext> mockedUserContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(marketController).build();
        mockedUserContext = Mockito.mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getRole).thenReturn(UserContext.UserRole.STANDARD);
    }

    @AfterEach
    void tearDown() {
        mockedUserContext.close();
    }

    @Test
    void testQuoteFound() throws Exception {
        QuoteDTO quote = new QuoteDTO();
        quote.setStockCode("600519");
        quote.setStockName("贵州茅台");
        quote.setLastPrice(new BigDecimal("1680.00"));
        quote.setChangeRate("+1.20%");

        when(marketService.getQuote("600519")).thenReturn(quote);

        mockMvc.perform(get("/market/quote/600519"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stockCode").value("600519"))
                .andExpect(jsonPath("$.data.lastPrice").value(1680.00))
                .andExpect(jsonPath("$.data.changeRate").value("+1.20%"));
    }

    @Test
    void testQuoteNotFound() throws Exception {
        when(marketService.getQuote("999999")).thenReturn(null);

        mockMvc.perform(get("/market/quote/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("股票未找到"));
    }

    @Test
    void testQuoteGuestMasked() throws Exception {
        // Override default STANDARD → GUEST for this test
        mockedUserContext.reset();
        mockedUserContext.when(UserContext::getRole).thenReturn(UserContext.UserRole.GUEST);

        QuoteDTO quote = new QuoteDTO();
        quote.setStockCode("600519");
        quote.setStockName("贵州茅台");
        quote.setLastPrice(new BigDecimal("1680.00"));
        quote.setTopBuyer(null);  // marketService 已经 mask 过了
        quote.setTopSeller(null);

        when(marketService.getQuote("600519")).thenReturn(quote);

        mockMvc.perform(get("/market/quote/600519"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.topBuyer").doesNotExist())
                .andExpect(jsonPath("$.data.topSeller").doesNotExist());
    }
}
