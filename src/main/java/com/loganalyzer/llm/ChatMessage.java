package com.loganalyzer.llm;

public record ChatMessage(Role role, String content) {

    public enum Role { SYSTEM, USER, ASSISTANT }

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content);
    }
}
