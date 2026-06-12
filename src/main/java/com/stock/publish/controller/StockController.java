package com.stock.publish.controller;

import com.stock.publish.service.StockService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/search")
    public Object search(@RequestParam String keyword) {
        // TODO: GET /api/publish/stock/search?keyword={xx}
        // 权限：GUEST，查 sync_stock_info，支持代码或拼音前缀，LIMIT 10
        throw new UnsupportedOperationException("TODO");
    }
}
