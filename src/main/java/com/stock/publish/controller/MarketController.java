package com.stock.publish.controller;

import com.stock.publish.calculation.KLineAggregator;
import com.stock.publish.calculation.TopTraderEngine;
import com.stock.publish.service.MarketService;
import com.stock.publish.dto.ApiResponse;
import com.stock.publish.interceptor.UserContext;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

/**
 * 市场行情数据对外暴露接口
 * 负责接收前端的图表数据请求，并在此层把关权限越界访问。
 */
@RestController
@RequestMapping("/market") 
public class MarketController {

    private final MarketService marketService;
    private final KLineAggregator kLineAggregator;
    private final TopTraderEngine topTraderEngine; // 注入供后续开发实时盘口 Quote 时使用

    public MarketController(MarketService marketService, KLineAggregator kLineAggregator, TopTraderEngine topTraderEngine) {
        this.marketService = marketService;
        this.kLineAggregator = kLineAggregator;
        this.topTraderEngine = topTraderEngine;
    }

    @GetMapping("/quote/{stockCode}")
    public Object quote(@PathVariable String stockCode) {
        // 权限：GUEST (仅价格) / STANDARD (附加 top_buyer, top_seller, 盘口)
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * 获取 K 线图核心数据 (GET /api/publish/market/kline)
     * 1. GUEST: 完全拦截，禁止访问 K 线数据。
     * 2. STANDARD: 仅放行 period = "1D" (日K线) 的请求。
     * 3. PREMIUM_VIP: 放行所有周期 (5M/1H/1D/1M/1Y)。
     * * @param stockCode 请求的股票代码
     * @param period    K线周期（默认 1D）
     */
    @GetMapping("/kline")
    public Object kline(@RequestParam String stockCode,
                        @RequestParam(defaultValue = "1D") String period) {
        
        UserContext.UserRole role = UserContext.getRole();
        String roleName = (role != null) ? role.name() : "GUEST"; // 防空指针保护，默认降级为 GUEST

        // --- 1. 越权拦截区 ---
        if ("GUEST".equals(roleName)) {
            return ApiResponse.fail(403, "游客无权查看K线图");
        }
        
        if ("STANDARD".equals(roleName) && !"1D".equals(period)) {
            return ApiResponse.fail(403, "普通用户仅可查看日K线(1D)");
        }

        // --- 2. 业务处理区 ---
        // 为了保护数据库与内存性能，默认限制查询范围为最近一年的数据
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusYears(1);

        return ApiResponse.ok(kLineAggregator.getKLineData(stockCode, period, start, end));
    }
}