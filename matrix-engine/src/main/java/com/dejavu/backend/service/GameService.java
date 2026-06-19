package com.dejavu.backend.service;

import com.dejavu.backend.model.*;
import com.dejavu.backend.dto.*;
import com.dejavu.backend.repository.*;
import com.dejavu.backend.ai.*;
import com.dejavu.backend.util.ActivityLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;

@Service
public class GameService {

    @Autowired
    private RoomSessionRepository roomSessionRepository;
    
    @Autowired
    private RoomBlueprintRepository blueprintRepository;
    
    @Autowired
    private ConfessionRepository confessionRepository;
    
    @Autowired
    private GuessAttemptRepository guessRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserAccountRepository userRepository;
    
    @Autowired
    private AiRoomBlueprintGenerator blueprintGenerator;
    
    @Autowired
    private AiGuessScorer guessScorer;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final int STARTING_COINS = 100;
    public static final int ROOM_WIN_COINS = 30;
    public static final int EXTRA_CLUE_COST_COINS = 20;
    public static final int MAX_FREE_CLUES = 2;
    public static final int MAX_TOTAL_CLUES = 4;
    public static final int ROOM_TIMER_SECONDS = 180;

    @Transactional
    public StartRoomResponse startRoom(Long userId) {
        Confession confession = confessionRepository.findRandomUnseenConfessionForUser(userId).orElse(null);

        if (confession == null) {
            // Either db is totally empty or user played everything. Let's create a dummy one or fail.
            long count = confessionRepository.count();
            if (count == 0) {
                Confession c = new Confession();
                c.setText("I slept with my best friend's boyfriend.");
                confession = confessionRepository.save(c);
            } else {
                throw new IllegalStateException("You have uncovered every dark secret in this town. Come back later.");
            }
        }
        
        UserAccount user = userService.getUser(userId).orElseThrow();
        
        final Confession finalConfession = confession;
        // Find existing blueprint for this confession AND language, or generate one
        RoomBlueprint blueprint = blueprintRepository.findFirstByConfessionIdAndLanguage(finalConfession.getId(), user.getPreferredLanguage())
            .orElseGet(() -> {
                RoomBlueprint bp = blueprintGenerator.generate(finalConfession, user.getPreferredLanguage());
                bp.setLanguage(user.getPreferredLanguage());
                return blueprintRepository.save(bp);
            });
            
        RoomSession session = new RoomSession();
        session.setUserId(userId);
        session.setConfessionId(finalConfession.getId());
        session.setRoomBlueprintId(blueprint.getId());
        session.setState("STARTED");
        session.setCurrentClueCount(0);
        session.setTimerStartedAt(LocalDateTime.now());
        session.setTimerDurationSeconds(ROOM_TIMER_SECONDS);
        session = roomSessionRepository.save(session);
        
        user.setRoomsEntered(user.getRoomsEntered() + 1);
        userRepository.save(user);
        
        ActivityLogger.log(user.getUsername(), "STARTED ROOM " + session.getId() + " - Confession ID: " + finalConfession.getId());
        
        StartRoomResponse resp = new StartRoomResponse();
        resp.setRoomSessionId(session.getId());
        resp.setRoomTitle(blueprint.getRoomTitle());
        resp.setBackgroundAssetId(blueprint.getBackgroundAssetId());
        
        try {
            List<String> objects = objectMapper.readValue(blueprint.getObjectAssetIdsJson(), new TypeReference<List<String>>(){});
            resp.setObjectAssetIds(objects);
        } catch (Exception e) {
            resp.setObjectAssetIds(new ArrayList<>());
        }
        
        resp.setOpeningAngelLine(blueprint.getOpeningAngelLine());
        resp.setTimerDurationSeconds(ROOM_TIMER_SECONDS);
        
        resp.setCoins(user.getCoins());
        
        return resp;
    }
    
    @Transactional
    public ClueResponse getReadyClue(Long sessionId) {
        RoomSession session = roomSessionRepository.findById(sessionId).orElseThrow();
        RoomBlueprint blueprint = blueprintRepository.findById(session.getRoomBlueprintId()).orElseThrow();
        
        if (session.getCurrentClueCount() == 0) {
            session.setCurrentClueCount(1);
            roomSessionRepository.save(session);
        }
        
        UserAccount user = userService.getUser(session.getUserId()).orElseThrow();
        user.setCluesUsed(user.getCluesUsed() + 1);
        userRepository.save(user);
        
        ActivityLogger.log(user.getUsername(), "FETCHED INITIAL CLUE for Room " + sessionId);
        
        return new ClueResponse(blueprint.getClue1(), session.getCurrentClueCount(), user.getCoins());
    }

    @Transactional
    public GuessResponse processGuess(Long sessionId, String guessText) {
        RoomSession session = roomSessionRepository.findById(sessionId).orElseThrow();
        RoomBlueprint blueprint = blueprintRepository.findById(session.getRoomBlueprintId()).orElseThrow();
        UserAccount user = userRepository.findById(session.getUserId()).orElseThrow();
        
        ActivityLogger.log(user.getUsername(), "GUESS ATTEMPT in Room " + sessionId + ": '" + guessText + "'");
        
        AiGuessScorer.ScoreResult score = guessScorer.scoreGuess(guessText, blueprint.getHiddenTargetSummary(), blueprint.getCustomScoringParams(), user.getPreferredLanguage());
        
        GuessAttempt attempt = new GuessAttempt();
        attempt.setRoomSessionId(sessionId);
        attempt.setGuessText(guessText);
        attempt.setAccuracy(score.accuracy);
        attempt.setTier(score.tier);
        attempt.setAngelFeedback(score.angelFeedback);
        
        GuessResponse resp = new GuessResponse();
        resp.setAccuracy(score.accuracy);
        resp.setTier(score.tier);
        resp.setWon(false);
        resp.setCoinsAwarded(0);
        resp.setAngelFeedback(score.angelFeedback);
        
        if (score.accuracy >= 80 && !"WON".equals(session.getState())) {
            session.setState("WON");
            session.setCompletedAt(LocalDateTime.now());
            
            // Award coins
            userService.addCoins(session.getUserId(), ROOM_WIN_COINS, "ROOM_WIN", sessionId);
            resp.setWon(true);
            resp.setCoinsAwarded(ROOM_WIN_COINS);
            resp.setAngelFeedback(blueprint.getWinAngelLine());
            
            Confession c = confessionRepository.findById(session.getConfessionId()).orElseThrow();
            resp.setActualConfession(c.getText());
            
            // Update stats
            user = userService.getUser(session.getUserId()).orElseThrow(); // reload
            user.setRoomsCompleted(user.getRoomsCompleted() + 1);
            user.setConfessionsCorrectlyGuessed(user.getConfessionsCorrectlyGuessed() + 1);
            user.setCurrentStreak(user.getCurrentStreak() + 1);
            user.setTotalScore(user.getTotalScore() + ROOM_WIN_COINS);
            if (user.getCurrentStreak() > user.getBestStreak()) {
                user.setBestStreak(user.getCurrentStreak());
            }
            userRepository.save(user);
            ActivityLogger.log(user.getUsername(), "WON ROOM " + sessionId + " with accuracy " + score.accuracy);
        } else {
            ActivityLogger.log(user.getUsername(), "FAILED GUESS in Room " + sessionId + " (Tier: " + score.tier + ", Acc: " + score.accuracy + ")");
            // Only update current streak to 0 if it's a completely wrong guess (like tier MISS)
            if ("MISS".equals(score.tier)) {
                user = userService.getUser(session.getUserId()).orElseThrow();
                user.setCurrentStreak(0);
                userRepository.save(user);
            }
        }
        
        guessRepository.save(attempt);
        roomSessionRepository.save(session);
        
        user = userService.getUser(session.getUserId()).orElseThrow();
        resp.setUpdatedUserStats(new UserAccountDto(user));
        return resp;
    }
    
    @Transactional
    public ClueResponse getNextClue(Long sessionId, NextClueRequest request) {
        RoomSession session = roomSessionRepository.findById(sessionId).orElseThrow();
        RoomBlueprint blueprint = blueprintRepository.findById(session.getRoomBlueprintId()).orElseThrow();
        
        if (session.getCurrentClueCount() >= MAX_TOTAL_CLUES) {
            throw new IllegalStateException("Maximum clues reached");
        }
        
        int nextClueNum = session.getCurrentClueCount() + 1;
        
        if (nextClueNum > MAX_FREE_CLUES) {
            UserAccount user = userService.getUser(session.getUserId()).orElseThrow();
            if ("COINS".equals(request.getMethod())) {
                boolean success = userService.deductCoins(session.getUserId(), EXTRA_CLUE_COST_COINS, "EXTRA_CLUE_SPEND", sessionId);
                if (!success) {
                    throw new IllegalStateException("Insufficient coins");
                }
                session.setCoinsSpentOnClues(session.getCoinsSpentOnClues() + EXTRA_CLUE_COST_COINS);
                user = userService.getUser(session.getUserId()).orElseThrow(); // reload
                user.setExtraCluesBoughtWithCoins(user.getExtraCluesBoughtWithCoins() + 1);
                ActivityLogger.log(user.getUsername(), "BOUGHT EXTRA CLUE with Coins in Room " + sessionId);
            } else if ("AD".equals(request.getMethod())) {
                session.setAdCluesUnlocked(session.getAdCluesUnlocked() + 1);
                user.setExtraCluesUnlockedByAds(user.getExtraCluesUnlockedByAds() + 1);
                ActivityLogger.log(user.getUsername(), "UNLOCKED EXTRA CLUE via AD in Room " + sessionId);
            } else {
                throw new IllegalArgumentException("Invalid method for extra clue");
            }
            userRepository.save(user);
        } else {
            UserAccount user = userService.getUser(session.getUserId()).orElseThrow();
            ActivityLogger.log(user.getUsername(), "FETCHED FREE CLUE #" + nextClueNum + " in Room " + sessionId);
        }
        
        session.setCurrentClueCount(nextClueNum);
        roomSessionRepository.save(session);
        
        String clueText = "";
        switch (nextClueNum) {
            case 2: clueText = blueprint.getClue2(); break;
            case 3: clueText = blueprint.getExtraClue3(); break;
            case 4: clueText = blueprint.getExtraClue4(); break;
        }
        
        UserAccount user = userService.getUser(session.getUserId()).orElseThrow();
        user.setCluesUsed(user.getCluesUsed() + 1);
        userRepository.save(user);
        
        return new ClueResponse(clueText, nextClueNum, user.getCoins());
    }

    @Transactional
    public GuessResponse expireRoom(Long sessionId) {
        RoomSession session = roomSessionRepository.findById(sessionId).orElseThrow();
        UserAccount user = userService.getUser(session.getUserId()).orElseThrow();
        
        GuessResponse resp = new GuessResponse();
        resp.setAccuracy(0);
        resp.setTier("MISS");
        resp.setWon(false);
        resp.setCoinsAwarded(0);
        
        if ("WON".equals(session.getState()) || "FAILED".equals(session.getState()) || "EXPIRED".equals(session.getState())) {
            resp.setUpdatedUserStats(new UserAccountDto(user));
            return resp;
        }
        
        session.setState("EXPIRED");
        session.setCompletedAt(LocalDateTime.now());
        roomSessionRepository.save(session);
        
        user.setRoomsFailed(user.getRoomsFailed() + 1);
        user.setCurrentStreak(0);
        userRepository.save(user);
        
        ActivityLogger.log(user.getUsername(), "EXPIRED ROOM " + sessionId + " due to timeout");
        
        resp.setAngelFeedback("The sand has gone silent. Time has escaped you.");
        
        Confession c = confessionRepository.findById(session.getConfessionId()).orElseThrow();
        resp.setActualConfession(c.getText());
        resp.setUpdatedUserStats(new UserAccountDto(user));
        
        return resp;
    }

    @Transactional
    public void pauseTimer(Long sessionId) {
        RoomSession session = roomSessionRepository.findById(sessionId).orElseThrow();
        // Simplified pause logic for phase 1. Track if needed.
    }

    @Transactional
    public void resumeTimer(Long sessionId) {
        RoomSession session = roomSessionRepository.findById(sessionId).orElseThrow();
        // Simplified resume logic for phase 1.
    }
}
