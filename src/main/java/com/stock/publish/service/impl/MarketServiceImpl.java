package com.stock.publish.service.impl;

import com.stock.publish.calculation.TopTraderEngine;
import com.stock.publish.dto.QuoteDTO;
import com.stock.publish.mapper.Kline5mDataMapper;
import com.stock.publish.mapper.SyncStockInfoMapper;
import com.stock.publish.service.MarketService;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MarketServiceImpl implements MarketService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final SyncStockInfoMapper stockInfoMapper;
    private final Kline5mDataMapper kline5mDataMapper;
    private final TopTraderEngine topTraderEngine;

    public MarketServiceImpl(StringRedisTemplate redisTemplate,
                             RedissonClient redissonClient,
                             SyncStockInfoMapper stockInfoMapper,
                             Kline5mDataMapper kline5mDataMapper,
                             TopTraderEngine topTraderEngine) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.stockInfoMapper = stockInfoMapper;
        this.kline5mDataMapper = kline5mDataMapper;
        this.topTraderEngine = topTraderEngine;
    }

    @Override
    public QuoteDTO getQuote(String stockCode) {
        // TODO: 1. 读 Redis "quote:{stockCode}"
        // TODO: 2. Cache Miss → Redisson 分布式锁 → 查库回填
        // TODO: 3. 根据 UserContext.getRole() 屏蔽敏感字段（GUEST 不返回主力/盘口）
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void refreshQuotes() {
        // TODO: @Scheduled(fixedRate=5000) 每5秒执行
        // TODO: 1. 从中央交易系统拉取最新成交流水（Mock）
        // TODO: 2. 计算 last_price、change_rate
        // TODO: 3. 写入 Redis "quote:{stockCode}" TTL 5s
        // TODO: 4. 调用 TopTraderEngine.accumulate() 累加主力数据
        // TODO: 5. 将 tick 推入 Redis List "tick:{stockCode}" 供 K线聚合
        throw new UnsupportedOperationException("TODO");
    }

    public void aggregate5mKline() {
        // TODO: @Scheduled(initialDelay=300000, fixedRate=300000) 每5分钟执行
        // TODO: 1. 从 Redis "tick:{stockCode}" 取出过去5分钟的所有 tick
        // TODO: 2. 聚合为 OHLCV
        // TODO: 3. 写入 kline_5m_data 表
        throw new UnsupportedOperationException("TODO");
    }
}
