# Stockies50 - NIFTY 50 ADX Technical Indicator Analyzer

## 📋 Overview

**Stockies50** is a Spring Boot web application that automatically calculates and displays **ADX (Average Directional Index)** technical indicators for all 50 stocks in the NIFTY 50 index. The application helps traders identify bullish and bearish trends in Indian stock market securities by analyzing real-time data from Yahoo Finance.

### What is ADX?

The **Average Directional Index (ADX)** is a technical analysis indicator used to measure the strength of a trend (not its direction). Key interpretation:
- **ADX > 25**: Strong trend present
- **ADX < 20**: Weak or ranging market
- **+DI > -DI**: Bullish trend
- **-DI > +DI**: Bearish trend

---

## 🏗️ Project Structure

```
stockies50/
├── src/main/java/com/dharshan/stockies50/
│   ├── Stockies50Application.java      # Main entry point with @EnableScheduling
│   ├── config/
│   │   ├── AppConfig.java              # Application configuration (PasswordEncoder bean)
│   │   └── SecurityConfig.java         # Spring Security configuration
│   ├── controller/
│   │   ├── AdxDataController.java      # REST endpoint for ADX results
│   │   ├── AuthController.java         # Login/Signup endpoints
│   │   ├── HomeController.java         # Home page controller
│   │   ├── InfoController.java         # Informational pages
│   │   └── ViewController.java         # Additional view routing
│   ├── model/
│   │   ├── AdxData.java                # JPA Entity for ADX calculations
│   │   ├── Stock.java                  # Stock information entity
│   │   └── User.java                   # User authentication entity
│   ├── repository/
│   │   ├── AdxDataRepository.java      # Data access for ADX data (custom queries)
│   │   ├── StockRepository.java        # Data access for stocks
│   │   └── UserRepository.java         # Data access for users
│   ├── service/
│   │   ├── AdxCalculationService.java  # Scheduled ADX calculation logic
│   │   ├── AdxDataService.java         # Service interface for ADX operations
│   │   ├── AdxDataServiceImpl.java     # Implementation of ADX service
│   │   ├── CustomUserDetailsService.java # User authentication & registration
│   │   ├── UserService.java            # User service interface (unused)
│   │   └── UserServiceImpl.java        # User service implementation (unused)
│   └── util/
│       └── Nifty50DataADX.java         # Utility: Yahoo Finance API client & ADX calculator
├── src/main/resources/
│   ├── application.properties          # Configuration (DB credentials via env vars)
│   ├── static/                         # CSS, JS, images
│   └── templates/                      # Thymeleaf HTML templates
│       ├── index.html                  # Landing page
│       ├── login.html                  # Login form
│       ├── signup.html                 # Registration form
│       ├── home.html                   # Dashboard after login
│       ├── results.html                # Bullish/Bearish stock display
│       └── about-*.html                # Informational pages
├── pom.xml                             # Maven dependencies
└── railway.json                        # Railway deployment configuration
```

---

## 🔄 Data Flow Architecture

### 1. **Data Ingestion Flow** (Automated Hourly)

```
┌─────────────────────────────────────────────────────────────────┐
│                    SCHEDULED TASK (Every Hour)                   │
│              AdxCalculationService.calculateAndStoreAdx()        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ITERATE THROUGH NIFTY 50 STOCKS               │
│  ["RELIANCE.NS", "TCS.NS", "HDFCBANK.NS", ... 50 symbols]       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              FETCH HISTORICAL DATA FROM YAHOO FINANCE            │
│   Nifty50DataADX.fetchStockData(symbol)                          │
│   GET https://query1.finance.yahoo.com/v8/finance/chart/        │
│       {SYMBOL}?interval=1h&range=10d                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PARSE JSON RESPONSE                           │
│   Extract: timestamp, high, low, close for each candle          │
│   Return: List<StockData>                                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CALCULATE ADX INDICATOR                       │
│   Nifty50DataADX.calculateAdx(symbol, stockDataList, period=14) │
│                                                                  │
│   Steps:                                                         │
│   1. Calculate True Range (TR)                                   │
│   2. Calculate +DM and -DM (Directional Movement)               │
│   3. Smooth TR, +DM, -DM using Wilder's smoothing (14 periods)  │
│   4. Calculate +DI and -DI                                       │
│   5. Calculate DX (Directional Index)                            │
│   6. Smooth DX to get ADX                                        │
│                                                                  │
│   Return: AdxResult { plusDI[], minusDI[], adx[], timestamps[] }│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EXTRACT LATEST VALUES                         │
│   - Get last calculated ADX, +DI, -DI values                    │
│   - Convert timestamp to LocalDateTime                           │
│   - Create AdxData entity                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PERSIST TO DATABASE                           │
│   adxDataRepository.save(adxData)                               │
│   Table: adx_data (symbol, plus_di, minus_di, adx, timestamp)   │
└─────────────────────────────────────────────────────────────────┘
```

### 2. **User Request Flow** (Viewing Results)

```
┌─────────────────────────────────────────────────────────────────┐
│                    USER ACCESS /results                          │
│              (Must be authenticated via Spring Security)         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              AdxDataController.showResults(Model)                │
│   - Calls adxDataService.getTop5Bullish()                        │
│   - Calls adxDataService.getTop5Bearish()                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              AdxDataServiceImpl.getTop5Bullish()                 │
│   - Executes custom SQL query via repository                     │
│   - Finds latest record per symbol WHERE adx > 25 AND +DI > -DI │
│   - Orders by ADX descending, limits to 5                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              AdxDataRepository.getTop5BullishStocksGrouped()     │
│   Native SQL Query:                                              │
│   WITH ranked_data AS (                                          │
│     SELECT *, ROW_NUMBER() OVER (                                │
│       PARTITION BY symbol ORDER BY timestamp DESC) AS rnk        │
│     FROM adx_data)                                               │
│   SELECT * FROM ranked_data                                      │
│   WHERE rnk = 1 AND adx > 25 AND plus_di > minus_di              │
│   ORDER BY adx DESC LIMIT 5                                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MAP RESULTS TO ENTITIES                       │
│   Convert Object[] rows to List<AdxData>                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    RENDER results.html TEMPLATE                  │
│   Display tables:                                                │
│   - Top 5 Bullish Stocks (strong uptrend)                        │
│   - Top 5 Bearish Stocks (strong downtrend)                      │
│   Shows: Symbol, ADX value, +DI, -DI, Timestamp                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3. **User Authentication Flow**

```
┌─────────────────────────────────────────────────────────────────┐
│                    NEW USER REGISTRATION                         │
│              GET /signup → POST /signup                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│          CustomUserDetailsService.registerUser(User)             │
│   1. Check if email already exists                               │
│   2. Validate password == confirmPassword                        │
│   3. Encode password using BCrypt                                │
│   4. Save to database                                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REDIRECT TO /login                            │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    EXISTING USER LOGIN                           │
│              POST /login (Spring Security default)               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│          CustomUserDetailsService.loadUserByUsername(email)      │
│   1. Find user by email in database                              │
│   2. Return Spring UserDetails with encoded password             │
│   3. Spring Security validates password match                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REDIRECT TO /home                             │
│   (configured in SecurityConfig.defaultSuccessUrl)              │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| **Framework** | Spring Boot 3.4.3 |
| **Language** | Java 21 |
| **Database** | MySQL |
| **ORM** | Spring Data JPA (Hibernate) |
| **Security** | Spring Security (BCrypt password encoding) |
| **Template Engine** | Thymeleaf |
| **Build Tool** | Maven |
| **External API** | Yahoo Finance API v8 |
| **JSON Processing** | org.json library |
| **Deployment** | Railway.app |

---

## 📊 Database Schema

### `adx_data` Table
```sql
CREATE TABLE adx_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    symbol VARCHAR(255) NOT NULL,
    plus_di DOUBLE NOT NULL,
    minus_di DOUBLE NOT NULL,
    adx DOUBLE NOT NULL,
    timestamp DATETIME NOT NULL
);
-- Missing indexes (see improvements section)
```

### `user` Table
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    password VARCHAR(255),
    confirm_password VARCHAR(255) -- Note: This shouldn't be stored
);
-- Missing unique constraint on email
```

### `stock` Table
```sql
CREATE TABLE stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    symbol VARCHAR(255),
    company_name VARCHAR(255)
);
-- Currently unused in the application
```

---

## 🚀 How to Run Locally

### Prerequisites
- Java 21+
- Maven 3.6+
- MySQL 8.0+

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd stockies50
   ```

2. **Configure environment variables**
   ```bash
   export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/stockies50"
   export SPRING_DATASOURCE_USERNAME="your_username"
   export SPRING_DATASOURCE_PASSWORD="your_password"
   ```

3. **Create database**
   ```sql
   CREATE DATABASE stockies50;
   ```

4. **Build the application**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

6. **Access the application**
   - Open browser: `http://localhost:8080`
   - Default port: 8080

---

## 📈 Key Features

### ✅ Implemented Features
- **Automated ADX Calculation**: Hourly scheduled task fetches and processes NIFTY 50 data
- **Real-time Trend Analysis**: Identifies top 5 bullish and bearish stocks
- **User Authentication**: Secure login/signup with BCrypt password hashing
- **Responsive UI**: Thymeleaf templates with modern styling
- **Informational Pages**: Educational content about stock markets and technical indicators
- **Yahoo Finance Integration**: Direct API integration for historical stock data

### ⚠️ Known Limitations
- No input validation on user registration
- Sequential API calls (slow processing ~50 seconds)
- No caching mechanism
- Potential race conditions in multi-instance deployments
- Missing database indexes
- CSRF protection disabled
- Debug logging of sensitive data

---

## 🔧 Configuration

### Environment Variables Required
```properties
SPRING_DATASOURCE_URL=jdbc:mysql://host:port/database
SPRING_DATASOURCE_USERNAME=username
SPRING_DATASOURCE_PASSWORD=password
```

### Scheduled Task Configuration
In `AdxCalculationService.java`:
```java
@Scheduled(fixedRate = 3600000) // Every hour
public void calculateAndStoreAdx() { ... }
```

### ADX Calculation Parameters
In `Nifty50DataADX.java`:
- **Period**: 14 (standard ADX period)
- **Data Range**: 10 days
- **Interval**: 1 hour candles
- **Threshold**: ADX > 25 (strong trend)

---

## 📝 NIFTY 50 Stock Symbols

The application tracks these 50 stocks:
```
ADANIENT, ADANIPORTS, APOLLOHOSP, ASIANPAINT, AXISBANK,
BAJAJ-AUTO, BAJFINANCE, BAJAJFINSV, BHARTIARTL, BPCL,
BRITANNIA, BEL, CIPLA, COALINDIA, DRREDDY, EICHERMOT,
GRASIM, HCLTECH, HDFCBANK, HDFCLIFE, HEROMOTOCO, HINDALCO,
HINDUNILVR, ICICIBANK, INDUSINDBK, INFY, ITC, JSWSTEEL,
KOTAKBANK, LT, M&M, MARUTI, NESTLEIND, NTPC, ONGC,
POWERGRID, RELIANCE, SBIN, SHRIRAMFIN, SUNPHARMA, SBILIFE,
TCS, TATAMOTORS, TATACONSUM, TATASTEEL, TECHM, TITAN,
TRENT, ULTRACEMCO, WIPRO
```
*(All suffixed with `.NS` for NSE exchange)*

---

## 🎯 Business Logic Summary

### Bullish Signal Criteria
1. ADX > 25 (strong trend)
2. +DI > -DI (upward momentum)
3. Latest timestamp for that symbol
4. Ranked by highest ADX

### Bearish Signal Criteria
1. ADX > 25 (strong trend)
2. -DI > +DI (downward momentum)
3. Latest timestamp for that symbol
4. Ranked by highest ADX

---

## 📦 Deployment

### Railway Deployment
The application is configured for Railway.app deployment via `railway.json`. Environment variables must be set in Railway dashboard.

### Procfile
```
web: java -jar target/stockies50-0.0.1-SNAPSHOT.jar
```

---

## 📚 Additional Resources

- **About Stock Market**: `/about-stock-market`
- **About NSE NIFTY 50**: `/about-nse-nifty50`
- **About Technical Indicators**: `/about-technical-indicators`

---

## 👨‍💻 Development Notes

### ADX Calculation Algorithm
The implementation follows **J. Welles Wilder's** original methodology:
1. **True Range (TR)**: Max of (High-Low, |High-PrevClose|, |Low-PrevClose|)
2. **Directional Movement (DM)**: 
   - +DM = High - PrevHigh (if > 0 and > LowDiff)
   - -DM = PrevLow - Low (if > 0 and > HighDiff)
3. **Smoothing**: Wilder's 14-period exponential smoothing
4. **Directional Indicators**: 
   - +DI = (Smoothed +DM / Smoothed TR) × 100
   - -DI = (Smoothed -DM / Smoothed TR) × 100
5. **DX**: |(+DI) - (-DI)| / |(+DI) + (-DI)| × 100
6. **ADX**: Smoothed average of DX over 14 periods

---

## 📄 License

This project is proprietary software. All rights reserved.

---

## 🤝 Contributing

For code review findings and improvement suggestions, please refer to the separate CODE_REVIEW.md document.

---

**Last Updated**: March 2025  
**Version**: 0.0.1-SNAPSHOT
