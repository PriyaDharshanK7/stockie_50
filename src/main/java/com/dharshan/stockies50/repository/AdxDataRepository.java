/*package com.dharshan.stockies50.repository;

import com.dharshan.stockies50.model.AdxData;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdxDataRepository extends JpaRepository<AdxData, Long> {

    @Query(value = "SELECT a.* " +
                   "FROM adx_data a " +
                   "WHERE a.timestamp = (SELECT MAX(b.timestamp) " +
                                          "FROM adx_data b " +
                                          "WHERE b.symbol = a.symbol) " +
                   "AND a.adx > 25 " +
                   "AND a.plus_di > a.minus_di " +
                   "ORDER BY a.adx DESC " +
                   "LIMIT 5", nativeQuery = true)
    List<AdxData> getTop5BullishStocks();

    @Query(value = "SELECT a.* " +
                   "FROM adx_data a " +
                   "WHERE a.timestamp = (SELECT MAX(b.timestamp) " +
                                          "FROM adx_data b " +
                                          "WHERE b.symbol = a.symbol) " +
                   "AND a.adx > 25 " +
                   "AND a.minus_di > a.plus_di " +
                   "ORDER BY a.adx DESC " +
                   "LIMIT 5", nativeQuery = true)
    List<AdxData> getTop5BearishStocks();

    // Bullish condition: adx > 25, plus_di > minus_di
    // Group by symbol so that each symbol appears only once.
    @Query(value = "SELECT a.symbol, MAX(a.timestamp) as timestamp, MAX(a.adx) as adx, " +
           "MAX(a.plus_di) as plus_di, MAX(a.minus_di) as minus_di " +
           "FROM adx_data a " +
           "WHERE a.adx > 25 AND a.plus_di > a.minus_di " +
           "GROUP BY a.symbol " +
           "ORDER BY MAX(a.adx) DESC " +
           "LIMIT 5", nativeQuery = true)
    List<Object[]> getTop5BullishStocksGrouped();

    

    // Bearish condition: adx > 25, minus_di > plus_di
    @Query(value = "SELECT a.symbol, MAX(a.timestamp) as timestamp, MAX(a.adx) as adx, " +
           "MAX(a.plus_di) as plus_di, MAX(a.minus_di) as minus_di " +
           "FROM adx_data a " +
           "WHERE a.adx > 25 AND a.minus_di > a.plus_di " +
           "GROUP BY a.symbol " +
           "ORDER BY MAX(a.adx) DESC " +
           "LIMIT 5", nativeQuery = true)
    List<Object[]> getTop5BearishStocksGrouped();
}*/
package com.dharshan.stockies50.repository;

import com.dharshan.stockies50.model.AdxData;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdxDataRepository extends JpaRepository<AdxData, Long> {

    @Query(value = "SELECT a.* " +
                   "FROM adx_data a " +
                   "JOIN (SELECT symbol, MAX(timestamp) AS latest_timestamp " +
                          "FROM adx_data GROUP BY symbol) b " +
                   "ON a.symbol = b.symbol AND a.timestamp = b.latest_timestamp " +
                   "WHERE a.adx > 25 " +
                   "AND a.plus_di > a.minus_di " +
                   "ORDER BY a.adx DESC " +
                   "LIMIT 5", nativeQuery = true)
    List<AdxData> getTop5BullishStocks();

    @Query(value = "SELECT a.* " +
                   "FROM adx_data a " +
                   "JOIN (SELECT symbol, MAX(timestamp) AS latest_timestamp " +
                          "FROM adx_data GROUP BY symbol) b " +
                   "ON a.symbol = b.symbol AND a.timestamp = b.latest_timestamp " +
                   "WHERE a.adx > 25 " +
                   "AND a.minus_di > a.plus_di " +
                   "ORDER BY a.adx DESC " +
                   "LIMIT 5", nativeQuery = true)
    List<AdxData> getTop5BearishStocks();

    @Query(value = "WITH ranked_data AS ( " +
                   "SELECT *, ROW_NUMBER() OVER (PARTITION BY symbol ORDER BY timestamp DESC) AS rnk " +
                   "FROM adx_data) " +
                   "SELECT symbol, timestamp, adx, plus_di, minus_di " +
                   "FROM ranked_data " +
                   "WHERE rnk = 1 AND adx > 25 AND plus_di > minus_di " +
                   "ORDER BY adx DESC " +
                   "LIMIT 5", nativeQuery = true)
    List<Object[]> getTop5BullishStocksGrouped();

    @Query(value = "WITH ranked_data AS ( " +
                   "SELECT *, ROW_NUMBER() OVER (PARTITION BY symbol ORDER BY timestamp DESC) AS rnk " +
                   "FROM adx_data) " +
                   "SELECT symbol, timestamp, adx, plus_di, minus_di " +
                   "FROM ranked_data " +
                   "WHERE rnk = 1 AND adx > 25 AND minus_di > plus_di " +
                   "ORDER BY adx DESC " +
                   "LIMIT 5", nativeQuery = true)
    List<Object[]> getTop5BearishStocksGrouped();
}
