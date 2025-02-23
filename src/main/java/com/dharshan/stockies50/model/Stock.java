package com.dharshan.stockies50.model;

import jakarta.persistence.*;

@Entity
@Table(name = "stocks")
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Example: "ADANIENT.NS"
    @Column(unique = true)
    private String symbol;
    
    private String name;
    
    public Stock() { }
    public Stock(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
    }
    // Getters and setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}