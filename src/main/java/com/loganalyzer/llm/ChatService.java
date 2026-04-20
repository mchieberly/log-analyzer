package com.loganalyzer.llm;

import com.loganalyzer.config.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ChatService {

    private final ChatCompletionClient client;
    private final String model;
    private final String systemPrompt;
    private final List<ChatMessage> history = new ArrayList<>();

    public ChatService(ChatCompletionClient client, String model, String systemPrompt) {
        this.client = client;
        this.model = model;
        this.systemPrompt = systemPrompt;
    }

    public static ChatService fromConfig() {
        Config cfg = Config.getInstance();
        ChatCompletionClient client = new OpenAIChatCompletionClient(cfg.getApiBaseUrl(), cfg.getApiKey());
        return new ChatService(client, cfg.getModel(), cfg.getSystemPrompt());
    }

    public synchronized String send(String userMessage, String logContent) {
        return sendStreaming(userMessage, logContent, null);
    }

    public synchronized String sendStreaming(String userMessage, String logContent, Consumer<String> onDelta) {
        if (history.isEmpty()) {
            history.add(ChatMessage.system(systemPrompt));
            String seeded = "Here is the log to analyze:\n<log>\n"
                    + (logContent == null ? "" : logContent)
                    + "\n</log>\n\n"
                    + userMessage;
            history.add(ChatMessage.user(seeded));
        } else {
            history.add(ChatMessage.user(userMessage));
        }
        List<ChatMessage> snapshot = Collections.unmodifiableList(new ArrayList<>(history));
        String reply = client.completeStreaming(snapshot, model, onDelta);
        history.add(ChatMessage.assistant(reply));
        return reply;
    }

    public synchronized List<ChatMessage> history() {
        return List.copyOf(history);
    }

    public synchronized void reset() {
        history.clear();
    }
}
