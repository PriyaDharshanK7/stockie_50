package com.dharshan.stockies50.model;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "adx_data")
public class AdxData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String symbol;
    private double plusDi;
    private double minusDi;
    private double adx;
    private LocalDateTime timestamp;
    
    public AdxData() { }
    public AdxData(String symbol, double plusDi, double minusDi, double adx, LocalDateTime timestamp) {
        this.symbol = symbol;
        this.plusDi = plusDi;
        this.minusDi = minusDi;
        this.adx = adx;
        this.timestamp = timestamp;
    }
    // Getters and setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public double getPlusDi() { return plusDi; }
    public void setPlusDi(double plusDi) { this.plusDi = plusDi; }
    public double getMinusDi() { return minusDi; }
    public void setMinusDi(double minusDi) { this.minusDi = minusDi; }
    public double getAdx() { return adx; }
    public void setAdx(double adx) { this.adx = adx; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp row) { 
        this.timestamp = row.toLocalDateTime(); 
    }
}