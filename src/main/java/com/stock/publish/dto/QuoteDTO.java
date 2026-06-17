package com.stock.publish.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuoteDTO {
    private String stockCode;
    private String stockName;
    private BigDecimal lastPrice;
    private BigDecimal yesterdayClose;
    private String changeRate;
    private Integer status;

    // STANDARD+ fields
    private TopTraderDTO topBuyer;
    private TopTraderDTO topSeller;
}
