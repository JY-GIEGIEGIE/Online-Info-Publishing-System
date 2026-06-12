package com.stock.publish.service.impl;

import com.stock.publish.entity.SyncStockInfo;
import com.stock.publish.mapper.SyncStockInfoMapper;
import com.stock.publish.service.StockService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockServiceImpl implements StockService {

    private final SyncStockInfoMapper stockInfoMapper;

    public StockServiceImpl(SyncStockInfoMapper stockInfoMapper) {
        this.stockInfoMapper = stockInfoMapper;
    }

    @Override
    public List<SyncStockInfo> search(String keyword) {
        // TODO: 模糊搜索：stock_code LIKE keyword OR pinyin_abbr LIKE keyword，LIMIT 10
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public SyncStockInfo getByCode(String stockCode) {
        // TODO: 根据股票代码查询
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void syncFromCentralSystem() {
        // TODO: 从中央交易系统全量同步股票基础信息，生成本地拼音缩写
        throw new UnsupportedOperationException("TODO");
    }
}
