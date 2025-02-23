package com.dharshan.stockies50.controller;
import com.dharshan.stockies50.model.AdxData;
import com.dharshan.stockies50.repository.AdxDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ViewController {

    @Autowired
    private AdxDataRepository adxDataRepository;
    
    @GetMapping("/")
    public String index(Model model) {
        // Fetch all ADX data
        List<AdxData> allData = adxDataRepository.findAll();
        // Filter records with ADX > 25
        List<AdxData> filtered = allData.stream()
                .filter(data -> data.getAdx() > 25)
                .collect(Collectors.toList());
                
        // Bullish: +DI > -DI; Bearish: +DI < -DI
        List<AdxData> bullish = filtered.stream()
                .filter(data -> data.getPlusDi() > data.getMinusDi())
                .sorted(Comparator.comparingDouble(AdxData::getAdx).reversed())
                .limit(5)
                .collect(Collectors.toList());
                
        List<AdxData> bearish = filtered.stream()
                .filter(data -> data.getPlusDi() < data.getMinusDi())
                .sorted(Comparator.comparingDouble(AdxData::getAdx).reversed())
                .limit(5)
                .collect(Collectors.toList());
                
        model.addAttribute("bullish", bullish);
        model.addAttribute("bearish", bearish);
        return "index";
    }
    
    /*@GetMapping("/login")
    public String login() {
        return "login";
    }*/
}
