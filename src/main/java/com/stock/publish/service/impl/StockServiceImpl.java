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
                .like(SyncStockInfo::getStockName, keyword)
                .or()
                .like(SyncStockInfo::getPinyinAbbr, keyword)
                .last("LIMIT 10");
        return stockInfoMapper.selectList(wrapper);
    }

    @Override
    public SyncStockInfo getByCode(String stockCode) {
        // DONE: 根据股票代码查询
        return stockInfoMapper.selectById(stockCode);
    }

    @Override
    public void syncFromCentralSystem() {
        // DONE: 从中央交易系统全量同步股票基础信息，生成本地拼音缩写
        // ★ 集成阶段需替换为真实 HTTP 调用：GET {central-trade}/api/v1/stock/list
        // ★ 拼音缩写需引入 pinyin4j 或 TinyPinyin 生成
        SyncStockInfo s1 = new SyncStockInfo();
        s1.setStockCode("600519");
        s1.setStockName("贵州茅台");
        s1.setStockType(0);
        s1.setYesterdayClose(new java.math.BigDecimal("1660.00"));
        s1.setLimitRate(new java.math.BigDecimal("0.1000"));
        s1.setStatus(0);
        s1.setPinyinAbbr("GZMT");
        stockInfoMapper.insert(s1);

        SyncStockInfo s2 = new SyncStockInfo();
        s2.setStockCode("000001");
        s2.setStockName("平安银行");
        s2.setStockType(0);
        s2.setYesterdayClose(new java.math.BigDecimal("12.30"));
        s2.setLimitRate(new java.math.BigDecimal("0.1000"));
        s2.setStatus(0);
        s2.setPinyinAbbr("PAYH");
        stockInfoMapper.insert(s2);
    }
}
