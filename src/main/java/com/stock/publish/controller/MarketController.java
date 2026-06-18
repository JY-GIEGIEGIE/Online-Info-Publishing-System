package com.stock.publish.controller;

import com.stock.publish.calculation.KLineAggregator;
import com.stock.publish.calculation.TopTraderEngine;
import com.stock.publish.dto.ApiResponse;
import com.stock.publish.dto.QuoteDTO;
import com.stock.publish.interceptor.UserContext;
import com.stock.publish.service.MarketService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/market")
public class MarketController {

    private final MarketService marketService;
    private final KLineAggregator kLineAggregator;
    private final TopTraderEngine topTraderEngine;

    public MarketController(MarketService marketService, KLineAggregator kLineAggregator,
                            TopTraderEngine topTraderEngine) {
        this.marketService = marketService;
        this.kLineAggregator = kLineAggregator;
        this.topTraderEngine = topTraderEngine;
    }

    @GetMapping("/quote/{stockCode}")
    public ApiResponse<QuoteDTO> quote(@PathVariable String stockCode) {
        QuoteDTO quote = marketService.getQuote(stockCode);
        if (quote == null) {
            return ApiResponse.fail(404, "股票未找到");
        }
        return ApiResponse.ok(quote);
    }

    @GetMapping("/kline")
    public ApiResponse<?> kline(@RequestParam String stockCode,
                                @RequestParam(defaultValue = "1D") String period) {
        UserContext.UserRole role = UserContext.getRole();

        if (role == UserContext.UserRole.GUEST) {
            return ApiResponse.fail(403, "游客无权查看K线图");
        }
        if (role == UserContext.UserRole.STANDARD && !"1D".equals(period)) {
            return ApiResponse.fail(403, "普通用户仅可查看日K线(1D)");
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusYears(1);
        return ApiResponse.ok(kLineAggregator.getKLineData(stockCode, period, start, end));
    }
}
