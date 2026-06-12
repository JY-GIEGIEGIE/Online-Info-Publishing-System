package com.stock.publish.calculation;

import com.stock.publish.dto.TopTraderDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TopTraderEngine {

    private final StringRedisTemplate redisTemplate;

    public TopTraderEngine(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void accumulate(String stockCode, String buyAccountId,
                           String sellAccountId, long quantity) {
        // TODO: 使用 Redis Hash HINCRBY 累加当日买卖量
        // Key: "top_buyer:{stockCode}:{date}" / "top_seller:{stockCode}:{date}"
        throw new UnsupportedOperationException("TODO");
    }

    public TopTraderDTO getTopBuyer(String stockCode) {
        // TODO: 从 Redis Hash 中排序，返回当日买入量最大的账户及数量
        throw new UnsupportedOperationException("TODO");
    }

    public TopTraderDTO getTopSeller(String stockCode) {
        // TODO: 从 Redis Hash 中排序，返回当日卖出量最大的账户及数量
        throw new UnsupportedOperationException("TODO");
    }
}
