package com.stock.publish.controller;

import com.stock.publish.calculation.KLineAggregator;
import com.stock.publish.service.MarketService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/market")
public class MarketController {

    private final MarketService marketService;
    private final KLineAggregator kLineAggregator;

    public MarketController(MarketService marketService, KLineAggregator kLineAggregator) {
        this.marketService = marketService;
        this.kLineAggregator = kLineAggregator;
    }

    @GetMapping("/quote/{stockCode}")
    public Object quote(@PathVariable String stockCode) {
        // TODO: GET /api/publish/market/quote/{stockCode}
        // 权限：GUEST (仅价格) / STANDARD (附加 top_buyer, top_seller, 盘口)
        throw new UnsupportedOperationException("TODO");
    }

    @GetMapping("/kline")
    public Object kline(@RequestParam String stockCode,
                        @RequestParam(defaultValue = "1D") String period) {
        // TODO: GET /api/publish/market/kline?stockCode={xx}&period={5M|1H|1D|1M|1Y}
        // 权限：STANDARD (仅1D) / PREMIUM_VIP (全尺度)
        // 返回格式：[{"time":"...","open":...,"close":...,"high":...,"low":...,"volume":...,"ma5":...,"ma10":...,"dif":...,"dea":...,"macdBar":...}]
        throw new UnsupportedOperationException("TODO");
    }
}
