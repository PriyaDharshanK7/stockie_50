package com.dharshan.stockies50.service;

import com.dharshan.stockies50.model.AdxData;
import java.util.List;

public interface AdxDataService {
    List<AdxData> getTop5Bullish();
    List<AdxData> getTop5Bearish();
}