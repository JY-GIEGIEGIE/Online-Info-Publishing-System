package com.stock.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.publish.calculation.TopTraderEngine;
import com.stock.publish.dto.QuoteDTO;
import com.stock.publish.entity.Kline5mData;
import com.stock.publish.entity.SyncStockInfo;
import com.stock.publish.interceptor.UserContext;
import com.stock.publish.mapper.Kline5mDataMapper;
import com.stock.publish.mapper.SyncStockInfoMapper;
import com.stock.publish.service.MarketService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

@Service
public class MarketServiceImpl implements MarketService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final SyncStockInfoMapper stockInfoMapper;
    private final Kline5mDataMapper kline5mDataMapper;
    private final TopTraderEngine topTraderEngine;
    private final ObjectMapper objectMapper;

    public MarketServiceImpl(StringRedisTemplate redisTemplate,
                             RedissonClient redissonClient,
                             SyncStockInfoMapper stockInfoMapper,
                             Kline5mDataMapper kline5mDataMapper,
                             TopTraderEngine topTraderEngine,
                             ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.stockInfoMapper = stockInfoMapper;
        this.kline5mDataMapper = kline5mDataMapper;
        this.topTraderEngine = topTraderEngine;
        this.objectMapper = objectMapper;
    }

    // 角色屏蔽方法
    private QuoteDTO maskByRole(QuoteDTO quote) {
        UserContext.UserRole role = UserContext.getRole();
        if (role == UserContext.UserRole.GUEST) {
            quote.setTopBuyer(null);
            quote.setTopSeller(null);
            quote.setBidPrice(null);
            quote.setAskPrice(null);
            quote.setBidVolume(null);
            quote.setAskVolume(null);
        }
        return quote;
    }

    private QuoteDTO buildQuote(String stockCode) {
        SyncStockInfo info = stockInfoMapper.selectById(stockCode);
        if (info == null) {
            return null;
        }

        // 用最近一根5分钟K线的收盘价作为 lastPrice；没有K线时退化为昨收
        BigDecimal lastPrice = info.getYesterdayClose();
        Kline5mData latest = kline5mDataMapper.selectOne(
                new LambdaQueryWrapper<Kline5mData>()
                        .eq(Kline5mData::getStockCode, stockCode)
                        .orderByDesc(Kline5mData::getPeriodStartTime)
                        .last("LIMIT 1"));
        if (latest != null && latest.getClosePrice() != null) {
            lastPrice = latest.getClosePrice();
        }

        // 涨跌幅：(lastPrice - yesterdayClose) / yesterdayClose * 100
        String changeRate = "0.00%";
        BigDecimal yClose = info.getYesterdayClose();
        if (yClose != null && yClose.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal rate = lastPrice.subtract(yClose)
                    .divide(yClose, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            changeRate = (rate.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                    + rate.setScale(2, RoundingMode.HALF_UP) + "%";
        }

        QuoteDTO dto = new QuoteDTO();
        dto.setStockCode(info.getStockCode());
        dto.setStockName(info.getStockName());
        dto.setLastPrice(lastPrice);
        dto.setYesterdayClose(info.getYesterdayClose());
        dto.setChangeRate(changeRate);
        dto.setStatus(info.getStatus());
        try {
            dto.setTopBuyer(topTraderEngine.getTopBuyer(stockCode));
            dto.setTopSeller(topTraderEngine.getTopSeller(stockCode));
        } catch (UnsupportedOperationException e) {
            // B3 尚未实现，主力数据暂缺
        }
        // bidPrice/askPrice/bidVolume/askVolume 来自实时撮合，由 refreshQuotes 写入 Redis，此处无数据源
        return dto;
    }

    @Override
    public QuoteDTO getQuote(String stockCode) {
        // DONE: 1. 读 Redis "quote:{stockCode}"
        // DONE: 2. Cache Miss → Redisson 分布式锁 → 查库回填
        // DONE: 3. 根据 UserContext.getRole() 屏蔽敏感字段（GUEST 不返回主力/盘口）
        String cacheKey = "quote:" + stockCode;
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                return maskByRole(objectMapper.readValue(cachedJson, QuoteDTO.class));
            }

            RLock lock = redissonClient.getLock("lock:quote:" + stockCode);
            try {
                if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                    try {
                        // double check：可能别的线程已经回填了
                        cachedJson = redisTemplate.opsForValue().get(cacheKey);
                        if (cachedJson != null) {
                            return maskByRole(objectMapper.readValue(cachedJson, QuoteDTO.class));
                        }
                        // 构建并回填
                        QuoteDTO quote = buildQuote(stockCode);
                        String json = objectMapper.writeValueAsString(quote);
                        redisTemplate.opsForValue().set(cacheKey, json, 5, TimeUnit.SECONDS);
                        return maskByRole(quote);
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 没抢到锁，等 100ms 再读一次缓存
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                return maskByRole(objectMapper.readValue(cachedJson, QuoteDTO.class));
            }
            return null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON处理失败，stockCode: " + stockCode, e);
        }
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
