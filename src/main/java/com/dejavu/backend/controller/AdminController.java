package com.dejavu.backend.controller;

import com.dejavu.backend.model.AppSession;
import com.dejavu.backend.model.Confession;
import com.dejavu.backend.model.RoomBlueprint;
import com.dejavu.backend.model.UserAccount;
import com.dejavu.backend.repository.AppSessionRepository;
import com.dejavu.backend.repository.ConfessionRepository;
import com.dejavu.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dejavu.backend.ai.OpenAiClient;
import com.dejavu.backend.ai.GeminiAiClient;
import com.dejavu.backend.ai.DarkArchangelInterviewEngine;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ConfessionRepository confessionRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private AppSessionRepository sessionRepository;

    @Autowired
    private com.dejavu.backend.repository.RoomSessionRepository roomSessionRepository;

    @Autowired
    private com.dejavu.backend.repository.GuessAttemptRepository guessAttemptRepository;

    @Autowired
    private com.dejavu.backend.repository.game.ConfessionGameContentRepository gameContentRepository;

    @DeleteMapping("/reset-users")
    public ResponseEntity<Void> resetUsers() {
        guessAttemptRepository.deleteAll();
        roomSessionRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reset-confessions")
    public ResponseEntity<Void> resetConfessions() {
        blueprintRepository.deleteAll();
        confessionRepository.deleteAll();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalConfessions = confessionRepository.count();
        stats.put("totalConfessions", totalConfessions);
        
        long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);
        
        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime yesterday = today.minusDays(1);
        LocalDateTime lastWeek = today.minusDays(7);
        
        List<UserAccount> users = userRepository.findAll();
        long activeToday = users.stream().filter(u -> u.getLastActiveAt() != null && u.getLastActiveAt().isAfter(today)).count();
        stats.put("activeToday", activeToday);
        
        long activeCurrent = users.stream().filter(u -> u.getLastActiveAt() != null && u.getLastActiveAt().isAfter(LocalDateTime.now().minusMinutes(15))).count();
        stats.put("activeCurrent", activeCurrent);

        List<AppSession> sessions = sessionRepository.findAll();
        
        // Time metrics (excluding un-interacted < 5s)
        long totalDuration = 0;
        long maxDuration = 0;
        long minDuration = Long.MAX_VALUE;
        int validSessions = 0;
        
        for (AppSession s : sessions) {
            if (s.isInteracted() || s.getDurationSeconds() > 5) {
                validSessions++;
                totalDuration += s.getDurationSeconds();
                if (s.getDurationSeconds() > maxDuration) maxDuration = s.getDurationSeconds();
                if (s.getDurationSeconds() < minDuration) minDuration = s.getDurationSeconds();
            }
        }
        
        if (validSessions > 0) {
            stats.put("avgScreenTime", totalDuration / validSessions);
            stats.put("maxScreenTime", maxDuration);
            stats.put("minScreenTime", minDuration);
        } else {
            stats.put("avgScreenTime", 0);
            stats.put("maxScreenTime", 0);
            stats.put("minScreenTime", 0);
        }
        
        // Room stats
        int totalRooms = 0;
        int maxRooms = 0;
        int minRooms = Integer.MAX_VALUE;
        
        for (UserAccount u : users) {
            totalRooms += u.getRoomsEntered();
            if (u.getRoomsEntered() > maxRooms) maxRooms = u.getRoomsEntered();
            if (u.getRoomsEntered() < minRooms) minRooms = u.getRoomsEntered();
        }
        
        if (!users.isEmpty()) {
            stats.put("avgRoomsVisited", totalRooms / users.size());
            stats.put("maxRoomsVisited", maxRooms);
            stats.put("minRoomsVisited", minRooms);
        } else {
            stats.put("avgRoomsVisited", 0);
            stats.put("maxRoomsVisited", 0);
            stats.put("minRoomsVisited", 0);
        }
        
        // More sophisticated splits would go here (DAU, MAU, Retention)
        stats.put("DAU", activeToday);
        stats.put("MAU", users.stream().filter(u -> u.getLastActiveAt() != null && u.getLastActiveAt().isAfter(today.minusDays(30))).count());
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/confessions")
    public ResponseEntity<List<Confession>> getConfessions() {
        return ResponseEntity.ok(confessionRepository.findAll());
    }

    @PostMapping("/confessions")
    public ResponseEntity<Confession> addConfession(@RequestBody Confession c) {
        return ResponseEntity.ok(confessionRepository.save(c));
    }

    @PutMapping("/confessions/{id}")
    public ResponseEntity<Confession> updateConfession(@PathVariable Long id, @RequestBody Confession c) {
        c.setId(id);
        return ResponseEntity.ok(confessionRepository.save(c));
    }

    @Autowired
    private com.dejavu.backend.repository.PromptConfigRepository promptConfigRepository;
    
    @Autowired
    private com.dejavu.backend.ai.ConfessionQualityGrader qualityGrader;
    
    @Autowired
    private com.dejavu.backend.repository.RoomBlueprintRepository blueprintRepository;
    
    @Autowired
    private com.dejavu.backend.ai.AiRoomBlueprintGenerator blueprintGenerator;

    @GetMapping("/prompts/{key}")
    public ResponseEntity<com.dejavu.backend.model.PromptConfig> getPrompt(@PathVariable String key) {
        return promptConfigRepository.findById(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/prompts/{key}")
    public ResponseEntity<com.dejavu.backend.model.PromptConfig> updatePrompt(@PathVariable String key, @RequestBody String content) {
        com.dejavu.backend.model.PromptConfig config = promptConfigRepository.findById(key)
                .orElse(new com.dejavu.backend.model.PromptConfig(key, content));
        config.setPromptContent(content);
        return ResponseEntity.ok(promptConfigRepository.save(config));
    }
    
    @PostMapping("/confessions/{id}/grade")
    public ResponseEntity<com.dejavu.backend.ai.ConfessionQualityGrader.GradingResult> gradeConfession(@PathVariable Long id) {
        Confession c = confessionRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(qualityGrader.gradeConfession(c.getText()));
    }
    
    @PostMapping("/confessions/{id}/regenerate")
    public ResponseEntity<RoomBlueprint> regenerateClues(@PathVariable Long id, @RequestParam(defaultValue = "English") String language) {
        Confession c = confessionRepository.findById(id).orElseThrow();
        // Delete existing blueprint
        blueprintRepository.findFirstByConfessionIdAndLanguage(id, language)
            .ifPresent(bp -> blueprintRepository.delete(bp));
            
        // Generate new
        RoomBlueprint bp = blueprintGenerator.generate(c, language);
        bp.setLanguage(language);
        return ResponseEntity.ok(blueprintRepository.save(bp));
    }

    @PostMapping("/confessions/auto-process-all")
    public ResponseEntity<String> autoProcessAll(@RequestParam(defaultValue = "English") String language) {
        List<Confession> confessions = confessionRepository.findAll();
        int processed = 0;
        for (Confession c : confessions) {
            if (blueprintRepository.findFirstByConfessionIdAndLanguage(c.getId(), language).isEmpty()) {
                // First grade it to determine spicy tag
                com.dejavu.backend.ai.ConfessionQualityGrader.GradingResult grade = qualityGrader.gradeConfession(c.getText());
                if (grade != null) {
                    c.setSpicy(grade.isSpicy);
                    confessionRepository.save(c);
                }
                
                RoomBlueprint bp = blueprintGenerator.generate(c, language);
                bp.setLanguage(language);
                blueprintRepository.save(bp);
                processed++;
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            }
        }
        return ResponseEntity.ok("Successfully auto-processed " + processed + " new confessions.");
    }

    @PostMapping("/confessions/force-process-all")
    public ResponseEntity<String> forceProcessAll(@RequestParam(defaultValue = "English") String language) {
        List<Confession> confessions = confessionRepository.findAll();
        new Thread(() -> {
            int processed = 0;
            for (Confession c : confessions) {
                // Delete existing blueprint
                blueprintRepository.findFirstByConfessionIdAndLanguage(c.getId(), language)
                    .ifPresent(bp -> blueprintRepository.delete(bp));
                
                // Grade it
                com.dejavu.backend.ai.ConfessionQualityGrader.GradingResult grade = qualityGrader.gradeConfession(c.getText());
                if (grade != null) {
                    c.setSpicy(grade.isSpicy);
                    confessionRepository.save(c);
                }
                
                RoomBlueprint bp = blueprintGenerator.generate(c, language);
                bp.setLanguage(language);
                blueprintRepository.save(bp);
                processed++;
                try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
            }
            System.out.println("Finished force-processing " + processed + " blueprints.");
        }).start();
        return ResponseEntity.ok("Started background force-processing of all blueprints...");
    }

    @Autowired
    private com.dejavu.backend.ai.JuicyConfessionEngine juicyConfessionEngine;

    @PostMapping("/confessions/generate-juicy")
    public ResponseEntity<List<Confession>> generateJuicyConfessions(@RequestParam String theme, @RequestParam(defaultValue = "5") int count, @RequestParam(required = false) String jobId) {
        return ResponseEntity.ok(juicyConfessionEngine.generateJuicyConfessions(theme, count, jobId));
    }

    @GetMapping("/confessions/juicy-status")
    public ResponseEntity<Map<String, String>> getJuicyStatus(@RequestParam String jobId) {
        Map<String, String> result = new HashMap<>();
        result.put("status", com.dejavu.backend.ai.JuicyConfessionEngine.jobStatuses.getOrDefault(jobId, "UNKNOWN"));
        return ResponseEntity.ok(result);
    }

    @Autowired
    private com.dejavu.backend.config.ScheduledTasks scheduledTasks;

    @PostMapping("/confessions/trigger-daily")
    public ResponseEntity<String> triggerDailyConfessions() {
        new Thread(() -> {
            boolean original = scheduledTasks.isAutoEnabled();
            scheduledTasks.setAutoEnabled(true);
            scheduledTasks.generateDailyConfessions();
            scheduledTasks.setAutoEnabled(original);
        }).start();
        return ResponseEntity.ok("Daily 30-confession generation triggered asynchronously.");
    }

    private static int globalMaxQuestions = 3;

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("autoEnabled", scheduledTasks.isAutoEnabled());
        settings.put("maxQuestions", globalMaxQuestions);
        return ResponseEntity.ok(settings);
    }

    @Autowired
    private OpenAiClient openAiClient;
    
    @Autowired
    private GeminiAiClient geminiClient;

    @GetMapping("/settings/advanced")
    public ResponseEntity<Map<String, Object>> getAdvancedSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("autoEnabled", scheduledTasks.isAutoEnabled());
        settings.put("maxQuestions", globalMaxQuestions);
        settings.put("gptModel", openAiClient.getGptModel());
        settings.put("geminiHeavy", geminiClient.getHeavyModel());
        settings.put("geminiLight", geminiClient.getLightModel());
        settings.put("aiEnabled", geminiClient.isAiEnabled());
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/settings/advanced")
    public ResponseEntity<String> updateAdvancedSettings(@RequestBody Map<String, Object> payload) {
        if (payload.containsKey("autoEnabled")) {
            scheduledTasks.setAutoEnabled(Boolean.parseBoolean(payload.get("autoEnabled").toString()));
        }
        if (payload.containsKey("maxQuestions")) {
            globalMaxQuestions = Integer.parseInt(payload.get("maxQuestions").toString());
        }
        if (payload.containsKey("gptModel")) {
            openAiClient.setGptModel(payload.get("gptModel").toString());
        }
        if (payload.containsKey("geminiHeavy")) {
            geminiClient.setHeavyModel(payload.get("geminiHeavy").toString());
        }
        if (payload.containsKey("geminiLight")) {
            geminiClient.setLightModel(payload.get("geminiLight").toString());
        }
        if (payload.containsKey("aiEnabled")) {
            geminiClient.setAiEnabled(Boolean.parseBoolean(payload.get("aiEnabled").toString()));
        }
        return ResponseEntity.ok("Settings updated successfully.");
    }

    @PostMapping("/settings/toggle-auto")
    public ResponseEntity<String> toggleAuto() {
        scheduledTasks.setAutoEnabled(!scheduledTasks.isAutoEnabled());
        return ResponseEntity.ok("Auto Generator is now " + (scheduledTasks.isAutoEnabled() ? "ENABLED" : "DISABLED"));
    }

    @PostMapping("/settings/max-questions")
    public ResponseEntity<String> setMaxQuestions(@RequestParam int max) {
        globalMaxQuestions = max;
        return ResponseEntity.ok("Max Questions set to " + max);
    }

    public static int getGlobalMaxQuestions() {
        return globalMaxQuestions;
    }

    @GetMapping("/confessions/spicy-stats")
    public ResponseEntity<Map<String, Object>> getSpicyStats() {
        long total = confessionRepository.count();
        long spicy = confessionRepository.findAll().stream().filter(Confession::isSpicy).count();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("spicy", spicy);
        stats.put("percentage", total > 0 ? (int)((spicy * 100.0) / total) : 0);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/confessions/{id}")
    public ResponseEntity<Void> deleteConfession(@PathVariable Long id) {
        confessionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/stats/sessions")
    public ResponseEntity<String> wipeSessions() {
        sessionRepository.deleteAll();
        return ResponseEntity.ok("All fake/testing app sessions have been wiped.");
    }

    @Autowired
    private DarkArchangelInterviewEngine archangelEngine;

    @PostMapping("/confessions/{id}/archangel")
    public ResponseEntity<Map<String, String>> triggerArchangel(@PathVariable Long id) {
        return confessionRepository.findById(id).map(confession -> {
            // Erase old data
            com.dejavu.backend.model.game.ConfessionGameContent existing = gameContentRepository.findFirstByConfessionId(confession.getId());
            if (existing != null) {
                gameContentRepository.delete(existing);
            }
            confession.setExtendedStory(null);
            confessionRepository.save(confession);

            new Thread(() -> {
                archangelEngine.generateGameContent(confession);
            }).start();
            
            Map<String, String> result = new HashMap<>();
            result.put("message", "Archangel successfully started judging the confession.");
            return ResponseEntity.accepted().body(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/confessions/archangel-all")
    public ResponseEntity<String> triggerArchangelAll() {
        new Thread(() -> {
            List<Confession> confessions = confessionRepository.findAll();
            for (Confession c : confessions) {
                boolean needsProcessing = false;
                com.dejavu.backend.model.game.ConfessionGameContent gc = gameContentRepository.findFirstByConfessionId(c.getId());
                
                if (c.getExtendedStory() == null || c.getExtendedStory().equals(c.getText())) {
                    needsProcessing = true;
                } else if (gc == null || gc.getFragments() == null || gc.getFragments().size() < 10) {
                    needsProcessing = true;
                }

                if (needsProcessing) {
                    try {
                        if (gc != null) {
                            gameContentRepository.delete(gc);
                        }
                        c.setExtendedStory(null);
                        confessionRepository.save(c);

                        archangelEngine.generateGameContent(c);
                        Thread.sleep(50000); // Wait 50 seconds between processing. Each confession uses 12 AI requests. Free tier limit is 15 requests per minute.
                    } catch (Exception e) {
                        System.err.println("Failed to process confession " + c.getId() + ": " + e.getMessage());
                    }
                }
            }
        }).start();
        return ResponseEntity.ok("Started background processing of all incomplete or unexpanded confessions.");
    }

    @PostMapping("/confessions/wipe-stories")
    public ResponseEntity<String> wipeAllStories() {
        List<Confession> confessions = confessionRepository.findAll();
        for (Confession c : confessions) {
            c.setExtendedStory(null);
            confessionRepository.save(c);
        }
        return ResponseEntity.ok("All extended stories have been wiped. You can now run Archangel to regenerate them.");
    }
}
