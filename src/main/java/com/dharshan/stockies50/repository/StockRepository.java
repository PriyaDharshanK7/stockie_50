package com.dharshan.stockies50.repository;
import com.dharshan.stockies50.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findBySymbol(String symbol);
}