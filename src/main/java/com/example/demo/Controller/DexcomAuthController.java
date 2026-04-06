package com.example.demo.Controller;

import com.example.demo.Service.DexcomApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DexcomAuthController {

    private final DexcomApiService dexcomApiService;

    public DexcomAuthController(DexcomApiService dexcomApiService) {
        this.dexcomApiService = dexcomApiService;
    }

    @GetMapping("/api/dexcom/callback")
    public String callback(@RequestParam("code") String code) {
        return dexcomApiService.exchangeCode(code);
    }
    @GetMapping("/api/dexcom/poll")
    public String manualPoll() {
        dexcomApiService.pollDexcomApi();
        return "Poll triggered";
    }
}