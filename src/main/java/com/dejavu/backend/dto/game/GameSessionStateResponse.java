package com.dejavu.backend.dto.game;

import java.util.List;

public class GameSessionStateResponse {
    private Long sessionId;
    private String status;
    private String turnPlayer;
    private List<GameCard> playerHand;
    private int opponentHandCount;
    private GameCard topDiscard;
    private List<Long> storyStripFragmentIds;
    private Long activeJudgmentId;
    private String activeJudgmentText;
    private String activeJudgmentTarget;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTurnPlayer() { return turnPlayer; }
    public void setTurnPlayer(String turnPlayer) { this.turnPlayer = turnPlayer; }

    public List<GameCard> getPlayerHand() { return playerHand; }
    public void setPlayerHand(List<GameCard> playerHand) { this.playerHand = playerHand; }

    public int getOpponentHandCount() { return opponentHandCount; }
    public void setOpponentHandCount(int opponentHandCount) { this.opponentHandCount = opponentHandCount; }

    public GameCard getTopDiscard() { return topDiscard; }
    public void setTopDiscard(GameCard topDiscard) { this.topDiscard = topDiscard; }

    public List<Long> getStoryStripFragmentIds() { return storyStripFragmentIds; }
    public void setStoryStripFragmentIds(List<Long> storyStripFragmentIds) { this.storyStripFragmentIds = storyStripFragmentIds; }

    public Long getActiveJudgmentId() { return activeJudgmentId; }
    public void setActiveJudgmentId(Long activeJudgmentId) { this.activeJudgmentId = activeJudgmentId; }

    public String getActiveJudgmentText() { return activeJudgmentText; }
    public void setActiveJudgmentText(String activeJudgmentText) { this.activeJudgmentText = activeJudgmentText; }

    public String getActiveJudgmentTarget() { return activeJudgmentTarget; }
    public void setActiveJudgmentTarget(String activeJudgmentTarget) { this.activeJudgmentTarget = activeJudgmentTarget; }
}
