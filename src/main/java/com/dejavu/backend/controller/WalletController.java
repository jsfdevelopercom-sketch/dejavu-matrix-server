package com.dejavu.backend.controller;

import com.dejavu.backend.dto.UserAccountDto;
import com.dejavu.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WalletController {

    @Autowired
    private UserService userService;

    @GetMapping("/wallet/{userId}")
    public ResponseEntity<UserAccountDto> getWallet(@PathVariable Long userId) {
        return userService.getUser(userId)
                .map(u -> ResponseEntity.ok(new UserAccountDto(u)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/stats/{userId}")
    public ResponseEntity<UserAccountDto> getStats(@PathVariable Long userId) {
        return getWallet(userId);
    }

    @PostMapping("/wallet/purchase/mock")
    public ResponseEntity<UserAccountDto> mockPurchase(@RequestBody java.util.Map<String, Object> request) {
        Long userId = ((Number) request.get("userId")).longValue();
        String packId = (String) request.get("packId");
        
        int coins = 0;
        if ("PACK_50".equals(packId)) coins = 50;
        else if ("PACK_100".equals(packId)) coins = 100;
        else if ("PACK_200".equals(packId)) coins = 200;
        else throw new IllegalArgumentException("Invalid packId");
        
        userService.addCoins(userId, coins, "PURCHASE", null);
        
        com.dejavu.backend.model.UserAccount user = userService.getUser(userId).orElseThrow();
        user.setPurchasedCoins(user.getPurchasedCoins() + coins);
        userService.saveUser(user);
        
        return ResponseEntity.ok(new UserAccountDto(user));
    }

    @PostMapping("/wallet/purchase/verify")
    public ResponseEntity<UserAccountDto> verifyPurchase(@RequestBody java.util.Map<String, Object> request) {
        // Future hook for Play Billing verification
        throw new IllegalStateException("Production billing verification not implemented yet");
    }
}
