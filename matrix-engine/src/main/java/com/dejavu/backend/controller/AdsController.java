package com.dejavu.backend.controller;

import com.dejavu.backend.dto.AdRewardCompleteRequest;
import com.dejavu.backend.model.AdReward;
import com.dejavu.backend.repository.AdRewardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/ads")
public class AdsController {

    @Autowired
    private AdRewardRepository adRewardRepository;

    @Autowired
    private com.dejavu.backend.service.UserService userService;
    
    @Autowired
    private com.dejavu.backend.repository.UserAccountRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${ADS_MOCK_ENABLED:true}")
    private boolean mockEnabled;

    @PostMapping("/reward/complete")
    public ResponseEntity<Map<String, String>> completeReward(@RequestBody AdRewardCompleteRequest request) {
        if (!mockEnabled) {
            throw new IllegalStateException("Mock ads are disabled in production.");
        }

        // Mock verification for phase 1
        AdReward reward = new AdReward();
        reward.setUserId(request.getUserId());
        reward.setRoomSessionId(request.getRoomSessionId());
        reward.setClueNumberUnlocked(request.getClueNumber());
        reward.setAdProvider("mock");
        reward.setAdCompleted(true);
        adRewardRepository.save(reward);
        
        com.dejavu.backend.model.UserAccount user = userRepository.findById(request.getUserId()).orElseThrow();
        user.setAdsWatched(user.getAdsWatched() + 1);
        userRepository.save(user);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        return ResponseEntity.ok(response);
    }
}
