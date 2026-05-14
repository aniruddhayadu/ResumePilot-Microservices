package com.resumepilot.ai.dto;

import java.util.List;

public class ChatRes {
    private List<Choice> choices;

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }

    public static class Choice {
        private Msg message;

        public Msg getMessage() { return message; }
        public void setMessage(Msg message) { this.message = message; }
    }
}