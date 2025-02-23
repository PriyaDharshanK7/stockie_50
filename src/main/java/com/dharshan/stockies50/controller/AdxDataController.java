package com.dharshan.stockies50.controller;

import com.dharshan.stockies50.model.AdxData;
import com.dharshan.stockies50.service.AdxDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class AdxDataController {

    @Autowired
    private AdxDataService adxDataService;

    @GetMapping("/results")
    public String showResults(Model model) {
        List<AdxData> bullishStocks = adxDataService.getTop5Bullish();
        List<AdxData> bearishStocks = adxDataService.getTop5Bearish();
        model.addAttribute("bullishStocks", bullishStocks);
        model.addAttribute("bearishStocks", bearishStocks);
        return "results"; // name of your Thymeleaf template
    }
}
