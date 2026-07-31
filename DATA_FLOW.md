# Stockies50 - Data Flow & Architecture Documentation

## 🎯 Application Purpose

**Stockies50** is an automated stock analysis tool that:
1. **Fetches** real-time historical data for all 50 NIFTY stocks from Yahoo Finance
2. **Calculates** ADX (Average Directional Index) technical indicators
3. **Identifies** the top 5 bullish and bearish stocks based on trend strength
4. **Displays** results to authenticated users via a web interface
5. **Runs automatically** every hour without manual intervention

---

## 📊 Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL DATA SOURCES                                │
│  ┌──────────────────┐                    ┌──────────────────┐               │
│  │  Yahoo Finance   │                    │     MySQL DB     │               │
│  │      API v8      │                    │                  │               │
│  └────────┬─────────┘                    └───────┬──────────┘               │
└───────────┼──────────────────────────────────────┼──────────────────────────┘
            │                                       │
            │ HTTP GET                              │ SQL INSERT/SELECT
            │ /v8/finance/chart/                    │                       │
            ▼                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      STOCKIES50 APPLICATION LAYER                            │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │                    SCHEDULED TASK (Hourly)                        │     │
│  │         @Scheduled(fixedRate = 3600000)                           │     │
│  │         AdxCalculationService.calculateAndStoreAdx()              │     │
│  └────────────────────────────────────────────────────────────────────┘     │
│                                │                                             │
│                                ▼                                             │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │                   UTIL LAYER (Nifty50DataADX)                     │     │
│  │                                                                    │     │
│  │  Step 1: fetchStockData(symbol)                                   │     │
│  │  ├─ Build URL: https://query1.finance.yahoo.com/.../{SYMBOL}      │     │
│  │  ├─ Send HTTP GET request with User-Agent header                  │     │
│  │  ├─ Receive JSON response                                         │     │
│  │  └─ Parse: timestamp[], high[], low[], close[] arrays             │     │
│  │      → Returns: List<StockData>                                   │     │
│  │                                                                    │     │
│  │  Step 2: calculateAdx(symbol, stockDataList, period=14)           │     │
│  │  ├─ Calculate True Range (TR) for each candle                     │     │
│  │  ├─ Calculate +DM and -DM (Directional Movement)                  │     │
│  │  ├─ Apply Wilder's smoothing (14 periods) to TR, +DM, -DM         │     │
│  │  ├─ Calculate +DI = (Smooth+DM / SmoothTR) × 100                  │     │
│  │  ├─ Calculate -DI = (Smooth-DM / SmoothTR) × 100                  │     │
│  │  ├─ Calculate DX = |+DI - -DI| / |+DI + -DI| × 100                │     │
│  │  └─ Smooth DX to get final ADX values                             │     │
│  │      → Returns: AdxResult { plusDI[], minusDI[], adx[], ts[] }    │     │
│  └────────────────────────────────────────────────────────────────────┘     │
│                                │                                             │
│                                ▼                                             │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │                  SERVICE LAYER                                     │     │
│  │                                                                    │     │
│  │  AdxCalculationService:                                           │     │
│  │  ├─ Iterate through 50 NIFTY symbols sequentially                 │     │
│  │  ├─ For each symbol:                                              │     │
│  │  │   • Fetch 10 days of 1-hour candle data                        │     │
│  │  │   • Calculate ADX with 14-period                               │     │
│  │  │   • Extract latest values (ADX, +DI, -DI, timestamp)           │     │
│  │  │   • Create AdxData entity                                      │     │
│  │  │   └─ Save to database                                          │     │
│  │  └─ Exception handling (printStackTrace)                          │     │
│  │                                                                    │     │
│  │  AdxDataService / AdxDataServiceImpl:                             │     │
│  │  ├─ getTop5Bullish():                                             │     │
│  │  │   • Query latest records per symbol                            │     │
│  │  │   • Filter: adx > 25 AND plus_di > minus_di                    │     │
│  │  │   • Order by adx DESC, limit 5                                 │     │
│  │  │                                                                  │     │
│  │  └─ getTop5Bearish():                                             │     │
│  │      • Query latest records per symbol                            │     │
│  │      • Filter: adx > 25 AND minus_di > plus_di                    │     │
│  │      • Order by adx DESC, limit 5                                 │     │
│  └────────────────────────────────────────────────────────────────────┘     │
│                                │                                             │
│                                ▼                                             │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │                  REPOSITORY LAYER                                  │     │
│  │                                                                    │     │
│  │  AdxDataRepository extends JpaRepository<AdxData, Long>           │     │
│  │                                                                    │     │
│  │  Custom Native Queries:                                           │     │
│  │  ┌────────────────────────────────────────────────────────────┐   │     │
│  │  │ getTop5BullishStocksGrouped():                             │   │     │
│  │  │ WITH ranked_data AS (                                      │   │     │
│  │  │   SELECT *,                                                │   │     │
│  │  │   ROW_NUMBER() OVER (                                      │   │     │
│  │  │     PARTITION BY symbol                                    │   │     │
│  │  │     ORDER BY timestamp DESC) AS rnk                        │   │     │
│  │  │   FROM adx_data                                            │   │     │
│  │  │ )                                                          │   │     │
│  │  │ SELECT symbol, timestamp, adx, plus_di, minus_di           │   │     │
│  │  │ FROM ranked_data                                           │   │     │
│  │  │ WHERE rnk = 1                                              │   │     │
│  │  │   AND adx > 25                                             │   │     │
│  │  │   AND plus_di > minus_di                                   │   │     │
│  │  │ ORDER BY adx DESC                                          │   │     │
│  │  │ LIMIT 5                                                    │   │     │
│  │  └────────────────────────────────────────────────────────────┘   │     │
│  │                                                                    │     │
│  │  Similar query for getTop5BearishStocksGrouped()                  │     │
│  │  (minus_di > plus_di condition)                                   │     │
│  └────────────────────────────────────────────────────────────────────┘     │
│                                │                                             │
│                                ▼                                             │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │                  CONTROLLER LAYER                                  │     │
│  │                                                                    │     │
│  │  AdxDataController:                                               │     │
│  │  @GetMapping("/results")                                          │     │
│  │  public String showResults(Model model) {                         │     │
│  │      List<AdxData> bullish = adxDataService.getTop5Bullish();     │     │
│  │      List<AdxData> bearish = adxDataService.getTop5Bearish();     │     │
│  │      model.addAttribute("bullishStocks", bullish);                │     │
│  │      model.addAttribute("bearishStocks", bearish);                │     │
│  │      return "results"; // Thymeleaf template                      │     │
│  │  }                                                                │     │
│  └────────────────────────────────────────────────────────────────────┘     │
│                                │                                             │
│                                ▼                                             │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │                  VIEW LAYER (Thymeleaf)                            │     │
│  │                                                                    │     │
│  │  results.html:                                                    │     │
│  │  ├─ Display table: Top 5 Bullish Stocks                           │     │
│  │  │   Columns: Symbol | ADX | +DI | -DI | Timestamp                │     │
│  │  │                                                                  │     │
│  │  └─ Display table: Top 5 Bearish Stocks                           │     │
│  │      Columns: Symbol | ADX | +DI | -DI | Timestamp                │     │
│  └────────────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────┘
            │                                       │
            │                                       │
            ▼                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         USER INTERACTION FLOW                                │
│                                                                              │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐           │
│  │  /login  │────▶│  /home   │────▶│ /results │────▶│  Logout  │           │
│  └──────────┘     └──────────┘     └──────────┘     └──────────┘           │
│       ▲                ▲                  ▲                                 │
│       │                │                  │                                 │
│  ┌────┴────┐      ┌────┴────┐       ┌────┴────┐                            │
│  │ /signup │      │Security │       │ Service │                            │
│  └─────────┘      │Filter   │       │ Layer   │                            │
│                   └─────────┘       └─────────┘                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Detailed Process Flows

### Flow 1: Automated ADX Calculation (Background Process)

```
TIME: Every hour (3600000 ms)
TRIGGER: Spring @Scheduled annotation
EXECUTOR: Single-threaded by default

Step-by-step:
┌─────────────────────────────────────────────────────────────┐
│ 1. START: calculateAndStoreAdx() method invoked             │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. LOOP: For each symbol in NIFTY50_SYMBOLS array (50 items)│
│    Example: "RELIANCE.NS", "TCS.NS", "HDFCBANK.NS", etc.    │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. TRY-CATCH block for error handling                       │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. FETCH: Nifty50DataADX.fetchStockData(symbol)             │
│    - URL: https://query1.finance.yahoo.com/v8/finance/      │
│           chart/RELIANCE.NS?interval=1h&range=10d           │
│    - Method: HTTP GET                                       │
│    - Headers: User-Agent: Mozilla/5.0                       │
│    - Response: JSON with timestamp[], quote[] arrays        │
│    - Parsing: Extract high, low, close for each candle      │
│    - Return: List<StockData> (typically ~240 candles)       │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. VALIDATE: Check if stockDataList.size() >= PERIOD + 1    │
│    If not enough data, skip to next symbol (continue)       │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. CALCULATE: Nifty50DataADX.calculateAdx(...)              │
│                                                          │
│    Input:                                                 │
│    - symbol: "RELIANCE.NS"                                │
│    - stockDataList: List of ~240 StockData objects        │
│    - period: 14                                           │
│                                                          │
│    Internal Arrays Created (size = stockDataList.size()): │
│    - tr[] (True Range)                                    │
│    - plusDM[] (+Directional Movement)                     │
│    - minusDM[] (-Directional Movement)                    │
│    - smoothTR[] (Smoothed True Range)                     │
│    - smoothPlusDI[]                                       │
│    - smoothMinusDI[]                                      │
│    - dx[] (Directional Index)                             │
│    - adx[] (Average Directional Index)                    │
│                                                          │
│    Calculation Steps:                                     │
│    A. Initialize timestamps list                          │
│    B. Loop i=1 to size-1:                                 │
│       - Calculate highDiff = high[i] - high[i-1]          │
│       - Calculate lowDiff = low[i-1] - low[i]             │
│       - Set plusDM[i] = highDiff if conditions met        │
│       - Set minusDM[i] = lowDiff if conditions met        │
│       - Calculate tr[i] = max(high, prevClose) -          │
│                         min(low, prevClose)               │
│                                                          │
│    C. Check if size > period (otherwise return empty)     │
│                                                          │
│    D. Initial smoothing at index=period:                  │
│       - smoothTR[period] = sum(tr[1..period])             │
│       - smoothPlusDI[period] = sum(plusDM[1..period])     │
│       - smoothMinusDI[period] = sum(minusDM[1..period])   │
│                                                          │
│    E. Loop i=period+1 to size-1:                          │
│       - Apply Wilder's smoothing:                         │
│         smoothTR[i] = smoothTR[i-1] - (smoothTR[i-1]/14)  │
│                     + tr[i]                               │
│       - Same for smoothPlusDI and smoothMinusDI           │
│       - Calculate +DI = (smoothPlusDI[i] / smoothTR[i])   │
│                       × 100                               │
│       - Calculate -DI = (smoothMinusDI[i] / smoothTR[i])  │
│                       × 100                               │
│       - Calculate DX = |+DI - -DI| / |+DI + -DI| × 100    │
│                                                          │
│    F. Calculate initial ADX at index=(period*2):          │
│       - Sum DX[period..period*2-1] and divide by period   │
│                                                          │
│    G. Loop i=(period*2)+1 to size-1:                      │
│       - Smooth ADX: adx[i] = ((adx[i-1] × 13) + dx[i]) /  │
│         14                                                │
│                                                          │
│    H. Calculate final +DI and -DI arrays for all indices  │
│                                                          │
│    Return: AdxResult object containing:                   │
│    - plusDI[]: array of +DI values                        │
│    - minusDI[]: array of -DI values                       │
│    - adx[]: array of ADX values                           │
│    - timestamps: List<Long> of candle timestamps          │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. EXTRACT: Get latest values from result arrays            │
│    - last = result.adx.length - 1                           │
│    - plusDi = result.plusDI[last]                           │
│    - minusDi = result.minusDI[last]                         │
│    - adx = result.adx[last]                                 │
│    - timestamp = convert result.timestamps.get(last) from   │
│      Unix epoch to LocalDateTime                            │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 8. CREATE ENTITY: new AdxData(symbol, plusDi, minusDi,      │
│    adx, timestamp)                                          │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 9. SAVE: adxDataRepository.save(adxData)                    │
│    - Hibernate generates INSERT SQL                         │
│    - Database: adx_data table                               │
│    - Columns: symbol, plus_di, minus_di, adx, timestamp     │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 10. CATCH: If exception occurs, print stack trace           │
│     (application continues to next symbol)                  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 11. REPEAT: Continue loop for next symbol                   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 12. END: All 50 symbols processed                           │
│     Estimated time: ~50 seconds (1 second per API call)     │
└─────────────────────────────────────────────────────────────┘
```

### Flow 2: User Views Results (Request-Response)

```
TIME: When user navigates to /results
TRIGGER: HTTP GET request
AUTHENTICATION: Required (Spring Security intercepts)

Step-by-step:
┌─────────────────────────────────────────────────────────────┐
│ 1. USER ACTION: Click "View Results" or navigate to /results│
│    Browser sends: GET /results HTTP/1.1                     │
│    Cookies: JSESSIONID=... (if logged in)                   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. SECURITY FILTER CHAIN intercepts request                 │
│    - Check if user is authenticated                         │
│    - If NOT authenticated: redirect to /login               │
│    - If authenticated: proceed to controller                │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. CONTROLLER: AdxDataController.showResults(Model model)   │
│    @GetMapping("/results")                                  │
│                                                             │
│    Execution:                                               │
│    - Injected: adxDataService (AdxDataServiceImpl instance) │
│    - Call: adxDataService.getTop5Bullish()                  │
│    - Call: adxDataService.getTop5Bearish()                  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. SERVICE: AdxDataServiceImpl.getTop5Bullish()             │
│                                                             │
│    Execution:                                               │
│    - Injected: adxDataRepository                            │
│    - Call: adxDataRepository.getTop5BullishStocksGrouped()  │
│    - Returns: List<Object[]> (raw database rows)            │
│                                                             │
│    Mapping Loop:                                            │
│    For each Object[] row:                                   │
│      - Create new AdxData()                                 │
│      - row[0] → setSymbol((String))                         │
│      - row[1] → setTimestamp((Timestamp))                   │
│      - row[2] → setAdx(((Number)).doubleValue())            │
│      - row[3] → setPlusDi(((Number)).doubleValue())         │
│      - row[4] → setMinusDi(((Number)).doubleValue())        │
│      - Add to result List<AdxData>                          │
│                                                             │
│    Return: List<AdxData> with 0-5 elements                  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. REPOSITORY: Execute native SQL query                     │
│                                                             │
│    SQL Query (getTop5BullishStocksGrouped):                 │
│    ┌─────────────────────────────────────────────────────┐  │
│    │ WITH ranked_data AS (                               │  │
│    │   SELECT *,                                         │  │
│    │   ROW_NUMBER() OVER (                               │  │
│    │     PARTITION BY symbol                             │  │
│    │     ORDER BY timestamp DESC                         │  │
│    │   ) AS rnk                                          │  │
│    │   FROM adx_data                                     │  │
│    │ )                                                   │  │
│    │ SELECT symbol, timestamp, adx, plus_di, minus_di    │  │
│    │ FROM ranked_data                                    │  │
│    │ WHERE rnk = 1                                       │  │
│    │   AND adx > 25                                      │  │
│    │   AND plus_di > minus_di                            │  │
│    │ ORDER BY adx DESC                                   │  │
│    │ LIMIT 5                                             │  │
│    └─────────────────────────────────────────────────────┘  │
│                                                             │
│    Query Explanation:                                       │
│    - CTE (Common Table Expression) creates ranked_data      │
│    - ROW_NUMBER() assigns rank 1 to latest record per symbol│
│    - PARTITION BY symbol: separate ranking for each stock   │
│    - ORDER BY timestamp DESC: newest gets rank 1            │
│    - WHERE rnk = 1: only latest records                     │
│    - AND adx > 25: only strong trends                       │
│    - AND plus_di > minus_di: only bullish (uptrend)         │
│    - ORDER BY adx DESC: strongest trends first              │
│    - LIMIT 5: top 5 results only                            │
│                                                             │
│    Database Operation:                                      │
│    - Full table scan on adx_data (no indexes!)              │
│    - Window function computation                            │
│    - Filtering and sorting                                  │
│    - Returns up to 5 rows with 5 columns each               │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. SERVICE: AdxDataServiceImpl.getTop5Bearish()             │
│    Same process as step 4-5, but different SQL condition:   │
│    WHERE rnk = 1 AND adx > 25 AND minus_di > plus_di        │
│    (identifies downtrends)                                  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. CONTROLLER: Populate model attributes                    │
│    model.addAttribute("bullishStocks", bullishList)         │
│    model.addAttribute("bearishStocks", bearishList)         │
│                                                             │
│    Return: "results" (logical view name)                    │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 8. VIEW RESOLVER: ThymeleafTemplateResolver                 │
│    - Resolves "results" to /templates/results.html          │
│    - Parses HTML template                                   │
│    - Processes Thymeleaf expressions:                       │
│      th:each="stock : ${bullishStocks}"                     │
│      th:text="${stock.symbol}"                              │
│      th:text="${stock.adx}"                                 │
│      etc.                                                   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 9. RENDER: Generate final HTML                              │
│    - Replace Thymeleaf tags with actual data                │
│    - Create HTML table rows for each stock                  │
│    - Include CSS, JavaScript, favicon                       │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 10. RESPONSE: Send HTML to browser                          │
│     HTTP/1.1 200 OK                                         │
│     Content-Type: text/html                                 │
│     <html>...</html>                                        │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 11. BROWSER: Render page for user                           │
│     Displays:                                               │
│     - Header with navigation                                │
│     - Table: Top 5 Bullish Stocks                           │
│       (Symbol | ADX | +DI | -DI | Timestamp)                │
│     - Table: Top 5 Bearish Stocks                           │
│       (Symbol | ADX | +DI | -DI | Timestamp)                │
│     - Footer                                                │
└─────────────────────────────────────────────────────────────┘
```

### Flow 3: User Registration & Authentication

```
┌─────────────────────────────────────────────────────────────┐
│ NEW USER REGISTRATION                                       │
└─────────────────────────────────────────────────────────────┘

Step 1: User accesses signup form
┌─────────────────────────────────────────────────────────────┐
│ GET /signup                                                 │
│ → AuthController.signupForm()                               │
│ → Creates new User object                                   │
│ → Returns "signup" view (signup.html)                       │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
Step 2: User submits registration form
┌─────────────────────────────────────────────────────────────┐
│ POST /signup                                                │
│ Form Data:                                                  │
│   - username: "john_doe"                                    │
│   - email: "john@example.com"                               │
│   - phone: "9876543210"                                     │
│   - password: "MySecurePass123"                             │
│   - confirmPassword: "MySecurePass123"                      │
│                                                             │
│ → AuthController.processSignup(User user)                   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
Step 3: Service validates and saves user
┌─────────────────────────────────────────────────────────────┐
│ CustomUserDetailsService.registerUser(user)                 │
│                                                             │
│ Validation Steps:                                           │
│ A. Check email uniqueness:                                  │
│    userRepository.findByEmail(email)                        │
│    → If present: return "Email already exists!"             │
│                                                             │
│ B. Check password match:                                    │
│    if (!password.equals(confirmPassword))                   │
│    → return "Password and Confirm Password do not match!"   │
│                                                             │
│ C. Encode password:                                         │
│    String encodedPassword =                                 │
│      passwordEncoder.encode(rawPassword)                    │
│    → Uses BCrypt algorithm (Spring Security default)        │
│    → Example: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWyWpWKaq │
│                                                             │
│    ⚠️ BUG: Debug print logs encoded password!               │
│    System.out.println("Encoded Password: " + encodedPassword│
│                                                             │
│ D. Set encoded password on user entity:                     │
│    user.setPassword(encodedPassword)                        │
│                                                             │
│ E. Save to database:                                        │
│    userRepository.save(user)                                │
│    → INSERT INTO user (username, email, phone, password)    │
│      VALUES (?, ?, ?, ?)                                    │
│                                                             │
│ F. Return "SUCCESS"                                         │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
Step 4: Redirect to login
┌─────────────────────────────────────────────────────────────┐
│ Controller receives "SUCCESS"                               │
│ → return "redirect:/login"                                  │
│ → Browser redirected to /login page                         │
└─────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────┐
│ EXISTING USER LOGIN                                         │
└─────────────────────────────────────────────────────────────┘

Step 1: User accesses login form
┌─────────────────────────────────────────────────────────────┐
│ GET /login                                                  │
│ → AuthController.loginForm()                                │
│ → Returns "login" view (login.html)                         │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
Step 2: User submits login credentials
┌─────────────────────────────────────────────────────────────┐
│ POST /login (handled by Spring Security)                    │
│ Form Data:                                                  │
│   - username (email): "john@example.com"                    │
│   - password: "MySecurePass123"                             │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
Step 3: Spring Security authenticates user
┌─────────────────────────────────────────────────────────────┐
│ AuthenticationManager.authenticate()                        │
│                                                             │
│ A. Load user details:                                       │
│    CustomUserDetailsService.loadUserByUsername(email)       │
│    → userRepository.findByEmail(email)                      │
│    → If not found: throw UsernameNotFoundException          │
│    → If found: create Spring UserDetails object             │
│       with email as username and encoded password           │
│                                                             │
│ B. Validate password:                                       │
│    passwordEncoder.matches(rawPassword, encodedPassword)    │
│    → BCrypt compares plain text with hash                   │
│    → If match: authentication successful                    │
│    → If no match: BadCredentialsException                   │
│                                                             │
│ C. Create SecurityContext:                                  │
│    - Store authenticated UserDetails                        │
│    - Create HTTP session                                    │
│    - Set JSESSIONID cookie                                  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
Step 4: Redirect to home page
┌─────────────────────────────────────────────────────────────┐
│ SecurityConfig configuration:                               │
│   .defaultSuccessUrl("/home", true)                         │
│                                                             │
│ → Browser redirected to /home                               │
│ → HomeController.home() returns "home" view                 │
│ → User sees dashboard with navigation to /results           │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database Interactions

### Write Operations (INSERT)

**When**: Every hour during scheduled task execution

**Frequency**: 50 INSERTs per hour (one per NIFTY stock)

**SQL Generated**:
```sql
INSERT INTO adx_data (symbol, plus_di, minus_di, adx, timestamp)
VALUES ('RELIANCE.NS', 28.5, 15.2, 32.4, '2025-03-15 14:00:00');
```

**Table Growth**:
- Per hour: 50 rows
- Per day: 1,200 rows
- Per month: 36,000 rows
- Per year: 438,000 rows

**⚠️ Issue**: No cleanup mechanism for old data. Table will grow indefinitely.

### Read Operations (SELECT)

**When**: Every time a user accesses `/results`

**Query Type**: Complex analytical query with window functions

**SQL Executed**:
```sql
-- For bullish stocks
WITH ranked_data AS (
  SELECT *, 
    ROW_NUMBER() OVER (
      PARTITION BY symbol 
      ORDER BY timestamp DESC
    ) AS rnk
  FROM adx_data
)
SELECT symbol, timestamp, adx, plus_di, minus_di
FROM ranked_data
WHERE rnk = 1 
  AND adx > 25 
  AND plus_di > minus_di
ORDER BY adx DESC
LIMIT 5;

-- Similar query for bearish stocks (minus_di > plus_di)
```

**Performance Characteristics**:
- Requires full table scan (no indexes)
- Window function computes row numbers for ALL records
- Filtering happens after window computation
- Sorting by ADX descending
- Performance degrades as table grows

**⚠️ Issue**: Missing composite index on (symbol, timestamp DESC)

---

## 🔐 Security Flow

### Request Filtering

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────┐
│ SecurityFilterChain                     │
│                                         │
│ Public URLs (permitAll):                │
│   /                                     │
│   /login                                │
│   /signup                               │
│   /about/**                             │
│   /css/**, /js/**, /images/**           │
│                                         │
│ Protected URLs (require auth):          │
│   /home                                 │
│   /results                              │
│   /admin/**                             │
└─────────────────────────────────────────┘
     │
     ├─→ Public URL → Proceed to controller
     │
     └─→ Protected URL
          │
          ├─→ Not authenticated → Redirect to /login
          │
          └─→ Authenticated → Proceed to controller
```

### CSRF Protection Status

**⚠️ CRITICAL ISSUE**: CSRF protection is DISABLED

```java
// SecurityConfig.java
http.csrf().disable()  // VULNERABLE TO CSRF ATTACKS
```

**Risk**: Attackers can trick authenticated users into performing unintended actions.

**Recommendation**: Enable CSRF protection or implement token-based authentication.

---

## ⏱️ Timing Analysis

### Scheduled Task Execution

| Phase | Time Estimate | Notes |
|-------|--------------|-------|
| Loop iteration setup | ~1ms per symbol | Negligible |
| HTTP request to Yahoo | ~500-1000ms per symbol | Network latency dependent |
| JSON parsing | ~50ms per symbol | In-memory operation |
| ADX calculation | ~10ms per symbol | CPU-bound, array operations |
| Database INSERT | ~10-50ms per symbol | Depends on DB load |
| Error handling | Variable | If exceptions occur |
| **Total per symbol** | **~570-1110ms** | |
| **Total for 50 symbols** | **~28-55 seconds** | Sequential processing |
| **Interval between runs** | **3600 seconds (1 hour)** | |

**Efficiency**: Only ~1.5% of time is used for computation. Rest is idle waiting.

### User Request Response Time

| Component | Time Estimate | Notes |
|-----------|--------------|-------|
| Security filter chain | ~5ms | Spring Security overhead |
| Controller method | ~1ms | Minimal logic |
| Service layer | ~2ms | Object mapping |
| Repository query | ~50-500ms | **Highly variable!** |
| View rendering | ~10-50ms | Thymeleaf template processing |
| **Total response time** | **~68-558ms** | Without caching |

**⚠️ Issue**: Query time increases linearly with table size due to missing indexes.

---

## 🚨 Bottlenecks & Issues Summary

### Performance Bottlenecks

1. **Sequential API Calls** (CRITICAL)
   - 50 stocks × ~1 second = ~50 seconds total
   - Could be parallelized to ~5 seconds with thread pool

2. **Missing Database Indexes** (HIGH)
   - No index on `symbol` column
   - No index on `timestamp` column
   - Queries require full table scans

3. **No Caching** (MEDIUM)
   - Same data fetched repeatedly from Yahoo Finance
   - Database queries executed on every page view

4. **Memory Allocation** (LOW)
   - New arrays allocated for each stock calculation
   - 8 arrays × 240 elements × 8 bytes = ~15KB per stock
   - Garbage collection pressure over time

### Logical Issues

1. **No Deduplication** (HIGH)
   - Scheduled task runs regardless of existing data
   - Multiple entries per symbol per hour possible

2. **No Data Retention Policy** (MEDIUM)
   - Table grows indefinitely
   - No archival or cleanup mechanism

3. **Race Conditions** (MEDIUM)
   - Multiple application instances would duplicate work
   - No distributed locking mechanism

4. **Silent Failures** (MEDIUM)
   - Exceptions caught and printed to console
   - No monitoring or alerting

### Security Issues

1. **CSRF Disabled** (CRITICAL)
2. **Password Debug Logging** (HIGH)
3. **No Input Validation** (MEDIUM)
4. **Duplicate User Service** (LOW)

---

## 📈 Scalability Considerations

### Current Capacity

- **Users**: Supports moderate concurrent users (limited by DB connections)
- **Stocks**: Fixed at 50 NIFTY symbols
- **Data Volume**: ~438K records/year
- **Processing Time**: ~50 seconds per hourly run

### Scaling Challenges

1. **Vertical Scaling Only**
   - Cannot horizontally scale scheduled tasks (no distributed locking)
   - Single point of failure

2. **Database Bottleneck**
   - All queries hit single MySQL instance
   - No read replicas configured

3. **API Rate Limits**
   - Yahoo Finance may throttle excessive requests
   - No rate limiting or backoff strategy

### Recommendations for Scale

1. Implement parallel processing with `ExecutorService`
2. Add Redis caching layer for API responses
3. Use ShedLock for distributed task scheduling
4. Partition database tables by date
5. Add connection pooling configuration
6. Implement circuit breaker for external API calls

---

**Document Version**: 1.0  
**Last Updated**: March 2025  
**Author**: Code Review Team
