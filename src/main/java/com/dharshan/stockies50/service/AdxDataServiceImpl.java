package com.dharshan.stockies50.service;

import com.dharshan.stockies50.model.AdxData;
import com.dharshan.stockies50.repository.AdxDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdxDataServiceImpl implements AdxDataService {

    @Autowired
    private AdxDataRepository adxDataRepository;

    @Override
    public List<AdxData> getTop5Bullish() {
        List<Object[]> rows = adxDataRepository.getTop5BullishStocksGrouped();
        List<AdxData> result = new ArrayList<>();
        for(Object[] row : rows) {
            AdxData data = new AdxData();
            data.setSymbol((String) row[0]);
            data.setTimestamp((java.sql.Timestamp) row[1]);
            data.setAdx(((Number) row[2]).doubleValue());
            data.setPlusDi(((Number) row[3]).doubleValue());
            data.setMinusDi(((Number) row[4]).doubleValue());
            result.add(data);
        }
        return result;
    }

    @Override
    public List<AdxData> getTop5Bearish() {
        List<Object[]> rows = adxDataRepository.getTop5BearishStocksGrouped();
        List<AdxData> result = new ArrayList<>();
        for(Object[] row : rows) {
            AdxData data = new AdxData();
            data.setSymbol((String) row[0]);
            data.setTimestamp((java.sql.Timestamp) row[1]);
            data.setAdx(((Number) row[2]).doubleValue());
            data.setPlusDi(((Number) row[3]).doubleValue());
            data.setMinusDi(((Number) row[4]).doubleValue());
            result.add(data);
        }
        return result;
    }
}
