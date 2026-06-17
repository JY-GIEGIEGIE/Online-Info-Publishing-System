package com.stock.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        // DONE: 模糊搜索：stock_code LIKE keyword OR pinyin_abbr LIKE keyword，LIMIT 10
        LambdaQueryWrapper<SyncStockInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(SyncStockInfo::getStockCode, keyword)
                .or()
                .like(SyncStockInfo::getPinyinAbbr, keyword)
                .last("LIMIT 10");
        return stockInfoMapper.selectList(wrapper);
    }

    @Override
    public SyncStockInfo getByCode(String stockCode) {
        // DONE: 根据股票代码查询
        LambdaQueryWrapper<SyncStockInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncStockInfo::getStockCode, stockCode);
        return stockInfoMapper.selectOne(wrapper);
    }

    @Override
    public void syncFromCentralSystem() {
        // TODO: 从中央交易系统全量同步股票基础信息，生成本地拼音缩写
        throw new UnsupportedOperationException("TODO");
    }
}
