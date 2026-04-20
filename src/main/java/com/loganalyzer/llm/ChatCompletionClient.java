package com.loganalyzer.llm;

import java.util.List;
import java.util.function.Consumer;

public interface ChatCompletionClient {

    String complete(List<ChatMessage> messages, String model);

    default String completeStreaming(List<ChatMessage> messages, String model, Consumer<String> onDelta) {
        String reply = complete(messages, model);
        if (onDelta != null && !reply.isEmpty()) {
            onDelta.accept(reply);
        }
        return reply;
    }
}
