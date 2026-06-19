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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@EnableScheduling
@Service
public class MarketServiceImpl implements MarketService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final SyncStockInfoMapper stockInfoMapper;
    private final Kline5mDataMapper kline5mDataMapper;
    private final TopTraderEngine topTraderEngine;
    private final ObjectMapper objectMapper;

    private static final List<String> MOCK_STOCKS = List.of("600519", "000001");

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

     private record TransactionRecord(
            String stockCode,
            LocalDateTime timestamp,
            String buyerAccount,
            String sellerAccount,
            BigDecimal price,
            long quantity
    ) {}

    @Scheduled(fixedRate = 5000)
    @Override
    public void refreshQuotes() {
        // DONE: 1. 从中央交易系统拉取最新成交流水（Mock）
        // DONE: 2. 计算 last_price、change_rate
        // DONE: 3. 写入 Redis "quote:{stockCode}" TTL 5s
        // DONE: 4. 调用 TopTraderEngine.accumulate() 累加主力数据
        // DONE: 5. 将 tick 推入 Redis List "tick:{stockCode}" 供 K线聚合
        List<TransactionRecord> records = mockTransactions();
        records.sort(Comparator.comparing(TransactionRecord::timestamp));

        // 按时间戳排序后 put，后出现的覆盖前一条 → 每只股票保留最新成交价
        Map<String, BigDecimal> lastPrices = new HashMap<>();
        for (TransactionRecord record : records) {
            if (record.stockCode != null) {
                lastPrices.put(record.stockCode, record.price);
            }
        }

        for (Map.Entry<String, BigDecimal> entry : lastPrices.entrySet()) {
            String code = entry.getKey();
            BigDecimal lastPrice = entry.getValue();

            SyncStockInfo info = stockInfoMapper.selectById(code);
            if (info == null) continue;

            // changeRate = (lastPrice - yesterdayClose) / yesterdayClose * 100%
            BigDecimal yClose = info.getYesterdayClose();
            BigDecimal rate = lastPrice.subtract(yClose)
                    .divide(yClose, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            String changeRate = (rate.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                    + rate + "%";

            // 组装 QuoteDTO 并写入 Redis（TTL 5 秒）
            QuoteDTO quote = buildQuote(code);
            quote.setLastPrice(lastPrice);
            quote.setChangeRate(changeRate);
            try {
                String json = objectMapper.writeValueAsString(quote);
                redisTemplate.opsForValue().set("quote:" + code, json, 5, TimeUnit.SECONDS);
            } catch (JsonProcessingException ignored) {}

            // 主力累加 & 推 tick
            for (TransactionRecord r : records) {
                if (!code.equals(r.stockCode)) continue;

                try {
                    topTraderEngine.accumulate(code, r.buyerAccount(), r.sellerAccount(), r.quantity());
                } catch (UnsupportedOperationException ignored) {}

                try {
                    String tickJson = objectMapper.writeValueAsString(r);
                    redisTemplate.opsForList().rightPush("tick:" + code, tickJson);
                } catch (JsonProcessingException ignored) {}
            }
        }
    }

    private List<TransactionRecord> mockTransactions() {
        List<TransactionRecord> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        // Mock 两只股票各一笔成交
        list.add(new TransactionRecord("600519", now.minusSeconds(2),
                "买方A", "卖方B", new BigDecimal("1680.00"), 5000L));
        list.add(new TransactionRecord("000001", now.minusSeconds(1),
                "买方C", "卖方D", new BigDecimal("12.50"), 30000L));
        return list;
    }

    @Scheduled(initialDelay = 300000, fixedRate = 300000)
    public void aggregate5mKline() {
        // DONE: 1. 从 Redis "tick:{stockCode}" 取出过去5分钟的所有 tick
        // DONE: 2. 聚合为 OHLCV
        // DONE: 3. 写入 kline_5m_data 表
        LocalDateTime periodStart = LocalDateTime.now()
                .minusMinutes(5)
                .truncatedTo(ChronoUnit.MINUTES);

        for (String code : MOCK_STOCKS) {
            String tickKey = "tick:" + code;
            List<String> tickJsons = redisTemplate.opsForList().range(tickKey, 0, -1);
            if (tickJsons == null || tickJsons.isEmpty()) continue;

            // JSON → TransactionRecord
            List<TransactionRecord> ticks = new ArrayList<>();
            for (String json : tickJsons) {
                try {
                    ticks.add(objectMapper.readValue(json, TransactionRecord.class));
                } catch (JsonProcessingException ignored) {}
            }
            if (ticks.isEmpty()) continue;

            // 聚合 OHLCV
            BigDecimal open = ticks.get(0).price();
            BigDecimal close = ticks.get(ticks.size() - 1).price();
            BigDecimal high = ticks.stream()
                    .map(TransactionRecord::price)
                    .max(BigDecimal::compareTo).orElse(open);
            BigDecimal low = ticks.stream()
                    .map(TransactionRecord::price)
                    .min(BigDecimal::compareTo).orElse(open);
            long volume = ticks.stream().mapToLong(TransactionRecord::quantity).sum();

            // 写入 kline_5m_data
            Kline5mData kline = new Kline5mData();
            kline.setStockCode(code);
            kline.setPeriodStartTime(periodStart);
            kline.setOpenPrice(open);
            kline.setClosePrice(close);
            kline.setHighPrice(high);
            kline.setLowPrice(low);
            kline.setVolume(volume);
            kline5mDataMapper.insert(kline);

            // 清空已消费的 tick
            redisTemplate.delete(tickKey);
        }
    }
}
