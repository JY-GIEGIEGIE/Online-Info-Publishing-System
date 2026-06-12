package com.stock.publish.calculation;

import com.stock.publish.dto.KLineDTO;
import com.stock.publish.mapper.Kline5mDataMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class KLineAggregator {

    private final Kline5mDataMapper kline5mDataMapper;

    public KLineAggregator(Kline5mDataMapper kline5mDataMapper) {
        this.kline5mDataMapper = kline5mDataMapper;
    }

    public List<KLineDTO> getKLineData(String stockCode, String period,
                                        LocalDateTime start, LocalDateTime end) {
        // TODO: 1. 从 kline_5m_data 表查出时间段内所有数据
        // TODO: 2. 若 period==5M → 直接映射为 KLineDTO
        // TODO: 3. 若 period==1H → 每12条5M数据聚合为1组
        // TODO: 4. 若 period==1D → 每48条5M数据聚合为1组
        // TODO: 5. 若 period==1M → 从1D聚合
        // TODO: 6. 若 period==1Y → 从1M聚合
        // TODO: 7. 聚合规则：Open取第一条、Close取最后一条、High取Max、Low取Min、Volume求和
        // TODO: 8. 调用 computeMA() 计算 MA5、MA10
        // TODO: 9. 调用 computeMACD() 计算 DIF、DEA、MACD柱
        throw new UnsupportedOperationException("TODO");
    }

    // TODO: private void computeMA(List<KLineDTO> data)
    // MA5 = 近5个周期收盘价均值; MA10 = 近10个周期收盘价均值

    // TODO: private void computeMACD(List<KLineDTO> data)
    // EMA12 = (close - prevEMA12) * (2/13) + prevEMA12
    // EMA26 = (close - prevEMA26) * (2/27) + prevEMA26
    // DIF = EMA12 - EMA26
    // DEA = (DIF - prevDEA) * (2/10) + prevDEA
    // MACD柱 = 2 * (DIF - DEA)
}
