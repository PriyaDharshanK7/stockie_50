package com.dharshan.stockies50.service;
import com.dharshan.stockies50.model.AdxData;
import com.dharshan.stockies50.repository.AdxDataRepository;
import com.dharshan.stockies50.util.Nifty50DataADX;
import com.dharshan.stockies50.util.Nifty50DataADX.StockData;
import com.dharshan.stockies50.util.Nifty50DataADX.AdxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AdxCalculationService {

    private static final String[] NIFTY50_SYMBOLS = {
        "ADANIENT.NS", "ADANIPORTS.NS", "APOLLOHOSP.NS", "ASIANPAINT.NS", "AXISBANK.NS", "BAJAJ-AUTO.NS",
        "BAJFINANCE.NS", "BAJAJFINSV.NS", "BHARTIARTL.NS", "BPCL.NS", "BRITANNIA.NS", "BEL.NS",
        "CIPLA.NS", "COALINDIA.NS", "DRREDDY.NS", "EICHERMOT.NS", "GRASIM.NS", "HCLTECH.NS",
        "HDFCBANK.NS", "HDFCLIFE.NS", "HEROMOTOCO.NS", "HINDALCO.NS", "HINDUNILVR.NS",
        "ICICIBANK.NS", "INDUSINDBK.NS", "INFY.NS", "ITC.NS", "JSWSTEEL.NS", "KOTAKBANK.NS",
        "LT.NS", "M&M.NS", "MARUTI.NS", "NESTLEIND.NS", "NTPC.NS", "ONGC.NS", "POWERGRID.NS",
        "RELIANCE.NS", "SBIN.NS", "SHRIRAMFIN.NS", "SUNPHARMA.NS", "SBILIFE.NS", "TCS.NS",
        "TATAMOTORS.NS", "TATACONSUM.NS", "TATASTEEL.NS", "TECHM.NS", "TITAN.NS", "TRENT.NS",
        "ULTRACEMCO.NS", "WIPRO.NS"
    };
    
    private static final int PERIOD = 14;
    
    @Autowired
    private AdxDataRepository adxDataRepository;
    
    // Run every hour (3600000 ms). Adjust as needed.
    @Scheduled(fixedRate = 3600000)
    public void calculateAndStoreAdx() {
        for (String symbol : NIFTY50_SYMBOLS) {
            try {
                List<StockData> stockDataList = Nifty50DataADX.fetchStockData(symbol);
                if (stockDataList.size() < PERIOD + 1) continue;
                AdxResult result = Nifty50DataADX.calculateAdx(symbol, stockDataList, PERIOD);
                int last = result.adx.length - 1;
                LocalDateTime timestamp = LocalDateTime.ofInstant(
                        new java.util.Date(result.timestamps.get(last) * 1000L).toInstant(),
                        ZoneId.systemDefault());
                AdxData adxData = new AdxData(symbol, result.plusDI[last], result.minusDI[last], result.adx[last], timestamp);
                adxDataRepository.save(adxData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}