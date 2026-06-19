package com.dejavu.backend.controller.game;

import com.dejavu.backend.ai.DarkArchangelInterviewEngine;
import com.dejavu.backend.model.Confession;
import java.util.List;
import java.util.ArrayList;
import com.dejavu.backend.model.game.ConfessionGameContent;
import com.dejavu.backend.repository.ConfessionRepository;
import com.dejavu.backend.repository.game.ConfessionGameContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/archangel/confessions")
public class ArchangelContentController {

    @Autowired
    private ConfessionRepository confessionRepository;

    @Autowired
    private ConfessionGameContentRepository gameContentRepository;

    @Autowired
    private DarkArchangelInterviewEngine archangelEngine;

    @PostMapping("/{confessionId}/generate-game-content")
    public ResponseEntity<?> generateGameContent(@PathVariable Long confessionId) {
        Confession confession = confessionRepository.findById(confessionId).orElse(null);
        if (confession == null) {
            return ResponseEntity.notFound().build();
        }
        
        new Thread(() -> {
            try {
                // Erase old data to force complete re-generation
                ConfessionGameContent existing = gameContentRepository.findFirstByConfessionId(confession.getId());
                if (existing != null) {
                    gameContentRepository.delete(existing);
                }
                confession.setExtendedStory(null);
                confessionRepository.save(confession);

                archangelEngine.generateGameContent(confession);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
        return ResponseEntity.accepted().body("Generation started in background for confession " + confessionId);
    }

    @GetMapping("/{confessionId}/game-content")
    public ResponseEntity<?> getGameContent(@PathVariable Long confessionId) {
        ConfessionGameContent content = gameContentRepository.findFirstByConfessionId(confessionId);
        if (content != null) {
            return ResponseEntity.ok(content);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/generate-all")
    public ResponseEntity<?> generateAllGameContent(@RequestParam(defaultValue = "false") boolean force) {
        new Thread(() -> {
            List<Confession> confessions = confessionRepository.findAll();
            for (Confession c : confessions) {
                ConfessionGameContent existing = gameContentRepository.findFirstByConfessionId(c.getId());
                boolean needsProcessing = force;
                
                if (c.getExtendedStory() == null || c.getExtendedStory().equals(c.getText())) {
                    needsProcessing = true;
                } else if (existing == null || existing.getFragments() == null || existing.getFragments().size() < 10) {
                    needsProcessing = true;
                }
                
                if (needsProcessing) {
                    try {
                        if (existing != null) {
                            gameContentRepository.delete(existing);
                        }
                        c.setExtendedStory(null);
                        confessionRepository.save(c);

                        archangelEngine.generateGameContent(c);
                        Thread.sleep(50000); // Wait 50s. AI takes heavy toll.
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        return ResponseEntity.ok("Batch generation started in background. Force: " + force);
    }
}
