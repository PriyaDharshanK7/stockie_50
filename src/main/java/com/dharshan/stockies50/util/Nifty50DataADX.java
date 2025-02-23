package com.dharshan.stockies50.util;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Nifty50DataADX {

    public static class StockData {
        public long timestamp;
        public double high;
        public double low;
        public double close;
        public StockData(long timestamp, double high, double low, double close) {
            this.timestamp = timestamp;
            this.high = high;
            this.low = low;
            this.close = close;
        }
    }
    
    public static class AdxResult {
        public double[] plusDI;
        public double[] minusDI;
        public double[] adx;
        public List<Long> timestamps;
        public AdxResult(double[] plusDI, double[] minusDI, double[] adx, List<Long> timestamps) {
            this.plusDI = plusDI;
            this.minusDI = minusDI;
            this.adx = adx;
            this.timestamps = timestamps;
        }
    }
    
    public static AdxResult calculateAdx(String symbol, List<StockData> stockDataList, int period) {
        int size = stockDataList.size();
        double[] tr = new double[size];
        double[] plusDM = new double[size];
        double[] minusDM = new double[size];
        double[] smoothTR = new double[size];
        double[] smoothPlusDI = new double[size];
        double[] smoothMinusDI = new double[size];
        double[] dx = new double[size];
        double[] adx = new double[size];
        List<Long> timestamps = new ArrayList<>();
        
        for (StockData d : stockDataList) {
            timestamps.add(d.timestamp);
        }
        
        for (int i = 1; i < size; i++) {
            double highDiff = stockDataList.get(i).high - stockDataList.get(i - 1).high;
            double lowDiff = stockDataList.get(i - 1).low - stockDataList.get(i).low;
            plusDM[i] = (highDiff > lowDiff && highDiff > 0) ? highDiff : 0;
            minusDM[i] = (lowDiff > highDiff && lowDiff > 0) ? lowDiff : 0;
            tr[i] = Math.max(stockDataList.get(i).high, stockDataList.get(i - 1).close)
                    - Math.min(stockDataList.get(i).low, stockDataList.get(i - 1).close);
        }
        if (size <= period) return new AdxResult(new double[0], new double[0], new double[0], timestamps);
        
        smoothTR[period] = sumArray(tr, 1, period);
        smoothPlusDI[period] = sumArray(plusDM, 1, period);
        smoothMinusDI[period] = sumArray(minusDM, 1, period);
        
        for (int i = period + 1; i < size; i++) {
            smoothTR[i] = smoothTR[i - 1] - (smoothTR[i - 1] / period) + tr[i];
            smoothPlusDI[i] = smoothPlusDI[i - 1] - (smoothPlusDI[i - 1] / period) + plusDM[i];
            smoothMinusDI[i] = smoothMinusDI[i - 1] - (smoothMinusDI[i - 1] / period) + minusDM[i];
            double plusDI = (smoothPlusDI[i] / smoothTR[i]) * 100;
            double minusDI = (smoothMinusDI[i] / smoothTR[i]) * 100;
            double diDiff = Math.abs(plusDI - minusDI);
            double diSum = plusDI + minusDI;
            dx[i] = (diSum == 0) ? 0 : (diDiff / diSum) * 100;
        }
        
        double adxSum = 0;
        for (int i = period; i < period * 2; i++) {
            adxSum += dx[i];
        }
        adx[period * 2] = adxSum / period;
        for (int i = (period * 2) + 1; i < size; i++) {
            adx[i] = ((adx[i - 1] * (period - 1)) + dx[i]) / period;
        }
        
        double[] finalPlusDI = new double[size];
        double[] finalMinusDI = new double[size];
        for (int i = 0; i < size; i++) {
            finalPlusDI[i] = (smoothPlusDI[i] / smoothTR[i]) * 100;
            finalMinusDI[i] = (smoothMinusDI[i] / smoothTR[i]) * 100;
        }
        return new AdxResult(finalPlusDI, finalMinusDI, adx, timestamps);
    }
    
    private static double sumArray(double[] array, int start, int end) {
        double sum = 0;
        for (int i = start; i <= end; i++) {
            sum += array[i];
        }
        return sum;
    }
    
    public static List<StockData> fetchStockData(String stockSymbol) {
        List<StockData> stockDataList = new ArrayList<>();
        try {
            String urlString = "https://query1.finance.yahoo.com/v8/finance/chart/" 
                    + stockSymbol + "?interval=1h&range=10d";
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            JSONObject jsonResponse = new JSONObject(content.toString());
            JSONObject chart = jsonResponse.getJSONObject("chart");
            JSONArray result = chart.getJSONArray("result");
            if(result.length() == 0) return stockDataList;
            JSONObject firstResult = result.getJSONObject(0);
            JSONArray timestamps = firstResult.getJSONArray("timestamp");
            JSONObject indicators = firstResult.getJSONObject("indicators")
                    .getJSONArray("quote").getJSONObject(0);
            JSONArray high = indicators.getJSONArray("high");
            JSONArray low = indicators.getJSONArray("low");
            JSONArray close = indicators.getJSONArray("close");
            for (int i = 0; i < timestamps.length(); i++) {
                long ts = timestamps.getLong(i);
                double highVal = high.optDouble(i, Double.NaN);
                double lowVal = low.optDouble(i, Double.NaN);
                double closeVal = close.optDouble(i, Double.NaN);
                if (!Double.isNaN(highVal) && !Double.isNaN(lowVal) && !Double.isNaN(closeVal)) {
                    stockDataList.add(new StockData(ts, highVal, lowVal, closeVal));
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return stockDataList;
    }
}