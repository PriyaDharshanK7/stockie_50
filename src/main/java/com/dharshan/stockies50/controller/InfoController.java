package com.dharshan.stockies50.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InfoController {

    @GetMapping("/about-stock-market")
    public String aboutStockMarket() {
        return "about-stock-market"; // This should map to about-stock-market.html in your templates folder.
    }

    @GetMapping("/about-nse-nifty50")
    public String aboutNseNifty50() {
        return "about-nse-nifty50";
    }

    @GetMapping("/about-technical-indicators")
    public String aboutTechnicalIndicators() {
        return "about-technical-indicators";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
    
}
