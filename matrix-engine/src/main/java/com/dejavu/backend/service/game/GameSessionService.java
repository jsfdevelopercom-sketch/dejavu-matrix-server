package com.dejavu.backend.service.game;

import com.dejavu.backend.dto.game.AnswerJudgmentRequest;
import com.dejavu.backend.dto.game.GameCard;
import com.dejavu.backend.dto.game.GameSessionStateResponse;
import com.dejavu.backend.dto.game.PlayCardRequest;
import com.dejavu.backend.model.game.ConfessionFragment;
import com.dejavu.backend.model.game.ConfessionGameContent;
import com.dejavu.backend.model.game.GameSession;
import com.dejavu.backend.model.game.JudgmentQuestion;
import com.dejavu.backend.repository.game.ConfessionGameContentRepository;
import com.dejavu.backend.repository.game.GameSessionRepository;
import com.dejavu.backend.repository.game.JudgmentQuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * GameSessionService
 * 
 * This is the central brain of the new Confession Card Battle game.
 * Features:
 * - Match Initiation (startAiMatch)
 * - Move Validation (playCard)
 * - AI Turn Execution (performAiTurn)
 * - Automatic Draw Pile Reshuffling
 * - Firestore multiplayer sync hook.
 */
@Service
public class GameSessionService {

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private ConfessionGameContentRepository contentRepository;

    @Autowired
    private JudgmentQuestionRepository judgmentQuestionRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * startAiMatch
     * Initiates a new match between a Human Player and an AI.
     */
    public GameSessionStateResponse startAiMatch(Long playerId) throws Exception {
        List<ConfessionGameContent> contents = contentRepository.findAll();
        if (contents.isEmpty()) {
            throw new IllegalStateException("No playable confessions available. Ensure Archangel has generated content.");
        }
        
        ConfessionGameContent content = contents.get(new Random().nextInt(contents.size()));

        List<GameCard> deck = new ArrayList<>();
        int counter = 0;
        for (ConfessionFragment frag : content.getFragments()) {
            // Original card
            GameCard card = new GameCard();
            card.setInstanceId(UUID.randomUUID().toString());
            card.setType("STANDARD");
            card.setFragmentId(frag.getId());
            card.setEmotionFamily(frag.getEmotionFamily());
            card.setIntensity(frag.getIntensity());
            card.setFragmentText(frag.getShortFragmentText());
            deck.add(card);
            
            // Duplicate card
            GameCard dup = new GameCard();
            dup.setInstanceId(UUID.randomUUID().toString());
            dup.setType("STANDARD");
            dup.setFragmentId(frag.getId());
            dup.setEmotionFamily(frag.getEmotionFamily());
            dup.setIntensity(frag.getIntensity());
            dup.setFragmentText(frag.getShortFragmentText());
            deck.add(dup);
        }
        
        Collections.shuffle(deck);

        List<GameCard> playerHand = new ArrayList<>();
        List<GameCard> aiHand = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            playerHand.add(deck.remove(0));
            aiHand.add(deck.remove(0));
        }
        
        GameCard topDiscard = deck.remove(0);

        GameSession session = new GameSession();
        session.setHumanPlayerId(playerId);
        session.setAiOpponentId(-1L);
        session.setStatus("ACTIVE");
        session.setTurnPlayer("PLAYER");
        session.setPlayerHand(mapper.writeValueAsString(playerHand));
        session.setAiHand(mapper.writeValueAsString(aiHand));
        session.setDrawPile(mapper.writeValueAsString(deck));
        session.setDiscardPile(mapper.writeValueAsString(Arrays.asList(topDiscard)));
        session.setStoryStripFragmentIds("[]");
        session.setCurrentFamily(topDiscard.getEmotionFamily());
        session.setCurrentIntensity(topDiscard.getIntensity());
        
        session = sessionRepository.save(session);
        syncStateToFirebase(session);
        return getState(session.getId());
    }

    public GameSessionStateResponse getState(Long sessionId) throws Exception {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow();
        GameSessionStateResponse res = new GameSessionStateResponse();
        res.setSessionId(session.getId());
        res.setStatus(session.getStatus());
        res.setTurnPlayer(session.getTurnPlayer());
        
        res.setPlayerHand(mapper.readValue(session.getPlayerHand(), new TypeReference<List<GameCard>>(){}));
        
        List<GameCard> aiHand = mapper.readValue(session.getAiHand(), new TypeReference<List<GameCard>>(){});
        res.setOpponentHandCount(aiHand.size());
        
        List<GameCard> discardPile = mapper.readValue(session.getDiscardPile(), new TypeReference<List<GameCard>>(){});
        res.setTopDiscard(discardPile.get(discardPile.size() - 1));
        
        res.setStoryStripFragmentIds(mapper.readValue(session.getStoryStripFragmentIds(), new TypeReference<List<Long>>(){}));
        
        res.setActiveJudgmentId(session.getActiveJudgmentId());
        res.setActiveJudgmentTarget(session.getActiveJudgmentTarget());
        
        if (session.getActiveJudgmentId() != null) {
            JudgmentQuestion jq = judgmentQuestionRepository.findById(session.getActiveJudgmentId()).orElse(null);
            if (jq != null) res.setActiveJudgmentText(jq.getText());
        }
        return res;
    }

    public GameSessionStateResponse playCard(Long sessionId, PlayCardRequest request) throws Exception {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow();
        if (!"ACTIVE".equals(session.getStatus()) || !"PLAYER".equals(session.getTurnPlayer())) {
            throw new IllegalStateException("Not your turn or game ended");
        }
        
        // Edge Case Fix: Prevent playing if a Judgment is pending for the Player
        if (session.getActiveJudgmentId() != null) {
            throw new IllegalStateException("You must answer the judgment first");
        }

        List<GameCard> playerHand = mapper.readValue(session.getPlayerHand(), new TypeReference<List<GameCard>>(){});
        List<GameCard> discardPile = mapper.readValue(session.getDiscardPile(), new TypeReference<List<GameCard>>(){});
        
        GameCard toPlay = null;
        for (GameCard c : playerHand) {
            if (c.getInstanceId().equals(request.getInstanceId())) {
                toPlay = c;
                break;
            }
        }
        if (toPlay == null) throw new IllegalStateException("Card not in hand");
        
        GameCard topDiscard = discardPile.get(discardPile.size() - 1);
        boolean isValid = toPlay.getEmotionFamily().equals(topDiscard.getEmotionFamily()) || 
                          toPlay.getIntensity().equals(topDiscard.getIntensity());
                          
        if (!isValid) throw new IllegalStateException("Invalid move: Must match Family or Intensity");
        
        playerHand.remove(toPlay);
        discardPile.add(toPlay);
        
        session.setPlayerHand(mapper.writeValueAsString(playerHand));
        session.setDiscardPile(mapper.writeValueAsString(discardPile));
        session.setCurrentFamily(toPlay.getEmotionFamily());
        session.setCurrentIntensity(toPlay.getIntensity());
        
        if (toPlay.getFragmentId() != null) {
            List<Long> storyStrip = mapper.readValue(session.getStoryStripFragmentIds(), new TypeReference<List<Long>>(){});
            if (!storyStrip.contains(toPlay.getFragmentId())) {
                storyStrip.add(toPlay.getFragmentId());
                session.setStoryStripFragmentIds(mapper.writeValueAsString(storyStrip));
            }
            
            // Trigger Judgment for the Opponent
            List<JudgmentQuestion> jqs = judgmentQuestionRepository.findAll();
            for (JudgmentQuestion jq : jqs) {
                if (jq.getFragment().getId().equals(toPlay.getFragmentId())) {
                    session.setActiveJudgmentId(jq.getId());
                    session.setActiveJudgmentTarget("AI");
                    break;
                }
            }
        }
        
        if (playerHand.isEmpty()) {
            session.setStatus("FINISHED");
            session.setWinnerId(session.getHumanPlayerId());
        } else {
            session.setTurnPlayer("AI");
        }
        
        session = sessionRepository.save(session);
        
        // If turn passes to AI, AI must play
        if ("AI".equals(session.getTurnPlayer())) {
            performAiTurn(session);
        }
        
        syncStateToFirebase(session);
        return getState(sessionId);
    }

    /**
     * Ensures we don't hit IndexOutOfBounds when draw pile is empty
     */
    private void reshuffleIfEmpty(List<GameCard> drawPile, List<GameCard> discardPile) {
        if (drawPile.isEmpty() && discardPile.size() > 1) {
            GameCard top = discardPile.remove(discardPile.size() - 1);
            drawPile.addAll(discardPile);
            Collections.shuffle(drawPile);
            discardPile.clear();
            discardPile.add(top);
        }
    }

    public GameSessionStateResponse drawCard(Long sessionId) throws Exception {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow();
        if (!"ACTIVE".equals(session.getStatus()) || !"PLAYER".equals(session.getTurnPlayer())) {
            throw new IllegalStateException("Not your turn or game ended");
        }
        if (session.getActiveJudgmentId() != null) {
            throw new IllegalStateException("You must answer the judgment first");
        }

        List<GameCard> playerHand = mapper.readValue(session.getPlayerHand(), new TypeReference<List<GameCard>>(){});
        List<GameCard> drawPile = mapper.readValue(session.getDrawPile(), new TypeReference<List<GameCard>>(){});
        List<GameCard> discardPile = mapper.readValue(session.getDiscardPile(), new TypeReference<List<GameCard>>(){});
        
        reshuffleIfEmpty(drawPile, discardPile);

        if (!drawPile.isEmpty()) {
            playerHand.add(drawPile.remove(0));
            session.setPlayerHand(mapper.writeValueAsString(playerHand));
            session.setDrawPile(mapper.writeValueAsString(drawPile));
            session.setDiscardPile(mapper.writeValueAsString(discardPile));
        }
        
        session.setTurnPlayer("AI");
        session = sessionRepository.save(session);
        
        // AI Turn
        performAiTurn(session);
        
        syncStateToFirebase(session);
        return getState(sessionId);
    }
    
    public GameSessionStateResponse answerJudgment(Long sessionId, boolean answer) throws Exception {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow();
        if (session.getActiveJudgmentId() == null || !"PLAYER".equals(session.getActiveJudgmentTarget())) {
            throw new IllegalStateException("No active judgment for you");
        }
        
        session.setActiveJudgmentId(null);
        session.setActiveJudgmentTarget(null);
        session = sessionRepository.save(session);
        syncStateToFirebase(session);
        return getState(sessionId);
    }

    /**
     * Executes the AI's logic
     * Fixes the soft-lock where AI previously did nothing.
     */
    private void performAiTurn(GameSession session) throws Exception {
        if (!"ACTIVE".equals(session.getStatus()) || !"AI".equals(session.getTurnPlayer())) return;
        
        // Simulate AI answering any Judgment instantly
        if (session.getActiveJudgmentId() != null && "AI".equals(session.getActiveJudgmentTarget())) {
            session.setActiveJudgmentId(null);
            session.setActiveJudgmentTarget(null);
        }
        
        List<GameCard> aiHand = mapper.readValue(session.getAiHand(), new TypeReference<List<GameCard>>(){});
        List<GameCard> discardPile = mapper.readValue(session.getDiscardPile(), new TypeReference<List<GameCard>>(){});
        List<GameCard> drawPile = mapper.readValue(session.getDrawPile(), new TypeReference<List<GameCard>>(){});
        
        GameCard topDiscard = discardPile.get(discardPile.size() - 1);
        
        // Find valid card
        GameCard toPlay = null;
        for (GameCard c : aiHand) {
            if (c.getEmotionFamily().equals(topDiscard.getEmotionFamily()) || c.getIntensity().equals(topDiscard.getIntensity())) {
                toPlay = c;
                break;
            }
        }
        
        if (toPlay != null) {
            // Play it
            aiHand.remove(toPlay);
            discardPile.add(toPlay);
            session.setAiHand(mapper.writeValueAsString(aiHand));
            session.setDiscardPile(mapper.writeValueAsString(discardPile));
            session.setCurrentFamily(toPlay.getEmotionFamily());
            session.setCurrentIntensity(toPlay.getIntensity());
            
            if (toPlay.getFragmentId() != null) {
                List<Long> storyStrip = mapper.readValue(session.getStoryStripFragmentIds(), new TypeReference<List<Long>>(){});
                if (!storyStrip.contains(toPlay.getFragmentId())) {
                    storyStrip.add(toPlay.getFragmentId());
                    session.setStoryStripFragmentIds(mapper.writeValueAsString(storyStrip));
                }
                
                // Trigger Judgment against PLAYER
                List<JudgmentQuestion> jqs = judgmentQuestionRepository.findAll();
                for (JudgmentQuestion jq : jqs) {
                    if (jq.getFragment().getId().equals(toPlay.getFragmentId())) {
                        session.setActiveJudgmentId(jq.getId());
                        session.setActiveJudgmentTarget("PLAYER");
                        break;
                    }
                }
            }
            
            if (aiHand.isEmpty()) {
                session.setStatus("FINISHED");
                session.setWinnerId(session.getAiOpponentId());
            } else {
                // If judgment is triggered against player, turn stays AI until player answers? 
                // No, standard Uno rule: Turn passes to Player. Player must answer before playing.
                session.setTurnPlayer("PLAYER");
            }
        } else {
            // Draw
            reshuffleIfEmpty(drawPile, discardPile);
            if (!drawPile.isEmpty()) {
                aiHand.add(drawPile.remove(0));
                session.setAiHand(mapper.writeValueAsString(aiHand));
                session.setDrawPile(mapper.writeValueAsString(drawPile));
                session.setDiscardPile(mapper.writeValueAsString(discardPile));
            }
            session.setTurnPlayer("PLAYER");
        }
        
        sessionRepository.save(session);
    }
    
    /**
     * syncStateToFirebase
     * 
     * Pushes the GameSession state to Firestore for real-time multiplayer listening.
     * Note: Wraps in try-catch to prevent crashing if Firebase credentials are missing.
     */
    private void syncStateToFirebase(GameSession session) {
        try {
            // Placeholder: If Firebase Admin SDK is configured:
            // Firestore db = FirestoreClient.getFirestore();
            // db.collection("game_sessions").document(session.getId().toString()).set(getState(session.getId()));
            System.out.println("Firebase Sync triggered for Session: " + session.getId());
        } catch (Exception e) {
            System.err.println("Firebase Sync failed (Likely missing credentials): " + e.getMessage());
        }
    }
}
