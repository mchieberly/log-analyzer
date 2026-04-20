package com.loganalyzer.llm;

import java.util.List;

public interface ChatCompletionClient {

    String complete(List<ChatMessage> messages, String model);
}
