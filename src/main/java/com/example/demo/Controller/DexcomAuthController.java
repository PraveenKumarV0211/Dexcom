package com.example.demo.Controller;

import com.example.demo.Service.DexcomApiService;
import com.example.demo.Service.GlucoseAlertConsumer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DexcomAuthController {

    private final DexcomApiService dexcomApiService;
    private final GlucoseAlertConsumer glucoseAlertConsumer;

    public DexcomAuthController(DexcomApiService dexcomApiService, GlucoseAlertConsumer glucoseAlertConsumer) {
        this.dexcomApiService = dexcomApiService;
        this.glucoseAlertConsumer = glucoseAlertConsumer;
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
    @GetMapping("/api/test/digest")
    public String testDigest() {
        glucoseAlertConsumer.sendDailyDigest();
        return "Digest sent";
    }
    @GetMapping("/api/dexcom/authorize")
    public String authorize() {
        String authUrl = "https://api.dexcom.com/v3/oauth2/login"
                + "?client_id=" + dexcomApiService.getClientId()
                + "&redirect_uri=" + dexcomApiService.getRedirectUri()
                + "&response_type=code"
                + "&scope=offline_access";
        return "<a href=\"" + authUrl + "\">Click here to authorize with Dexcom</a>";
    }
}