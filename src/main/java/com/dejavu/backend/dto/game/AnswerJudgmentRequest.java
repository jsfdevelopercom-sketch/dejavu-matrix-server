package com.dejavu.backend.dto.game;

public class AnswerJudgmentRequest {
    private Long questionId;
    private Boolean answer;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Boolean getAnswer() { return answer; }
    public void setAnswer(Boolean answer) { this.answer = answer; }
}
