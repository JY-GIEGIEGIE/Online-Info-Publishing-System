package com.stock.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stock.publish.entity.SyncStockInfo;
import com.stock.publish.mapper.SyncStockInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private SyncStockInfoMapper stockInfoMapper;

    @InjectMocks
    private StockServiceImpl stockService;

    @Test
    void testSearchByStockCode() {
        SyncStockInfo stock = new SyncStockInfo();
        stock.setStockCode("600519");
        stock.setStockName("贵州茅台");
        stock.setPinyinAbbr("GZMT");
        stock.setYesterdayClose(new BigDecimal("1660.00"));
        stock.setLimitRate(new BigDecimal("0.1000"));
        stock.setStatus(0);

        when(stockInfoMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(stock));

        List<SyncStockInfo> result = stockService.search("600");

        assertEquals(1, result.size());
        assertEquals("600519", result.get(0).getStockCode());
        assertEquals("贵州茅台", result.get(0).getStockName());
    }

    @Test
    void testSearchByPinyin() {
        SyncStockInfo stock = new SyncStockInfo();
        stock.setStockCode("600519");
        stock.setStockName("贵州茅台");
        stock.setPinyinAbbr("GZMT");

        when(stockInfoMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(stock));

        List<SyncStockInfo> result = stockService.search("GZMT");

        assertEquals(1, result.size());
        assertEquals("600519", result.get(0).getStockCode());
    }

    @Test
    void testSearchNoResult() {
        when(stockInfoMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<SyncStockInfo> result = stockService.search("zzzz");

        assertTrue(result.isEmpty());
    }
}
