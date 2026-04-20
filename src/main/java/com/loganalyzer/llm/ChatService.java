package com.loganalyzer.llm;

import com.loganalyzer.config.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        if (history.isEmpty()) {
            history.add(ChatMessage.system(systemPrompt));
            String seededLog = "<log>\n" + (logContent == null ? "" : logContent) + "\n</log>";
            history.add(ChatMessage.user(seededLog));
        }
        history.add(ChatMessage.user(userMessage));
        String reply = client.complete(Collections.unmodifiableList(new ArrayList<>(history)), model);
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
