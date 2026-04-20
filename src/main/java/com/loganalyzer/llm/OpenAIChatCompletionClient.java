package com.loganalyzer.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionAssistantMessageParam;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;
import com.openai.models.ChatModel;

import java.util.List;
import java.util.function.Consumer;

public class OpenAIChatCompletionClient implements ChatCompletionClient {

    private final OpenAIClient client;

    public OpenAIChatCompletionClient(String baseUrl, String apiKey) {
        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder().apiKey(apiKey);
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        this.client = builder.build();
    }

    @Override
    public String complete(List<ChatMessage> messages, String model) {
        ChatCompletion completion = client.chat().completions().create(buildParams(messages, model));
        return completion.choices().stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElse("");
    }

    @Override
    public String completeStreaming(List<ChatMessage> messages, String model, Consumer<String> onDelta) {
        StringBuilder full = new StringBuilder();
        StreamResponse<ChatCompletionChunk> stream =
                client.chat().completions().createStreaming(buildParams(messages, model));
        try {
            stream.stream().forEach(chunk -> chunk.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.delta().content())
                    .ifPresent(delta -> {
                        if (!delta.isEmpty()) {
                            full.append(delta);
                            if (onDelta != null) {
                                onDelta.accept(delta);
                            }
                        }
                    }));
        } finally {
            try {
                stream.close();
            } catch (Exception ignored) {
            }
        }
        return full.toString();
    }

    private ChatCompletionCreateParams buildParams(List<ChatMessage> messages, String model) {
        ChatCompletionCreateParams.Builder params = ChatCompletionCreateParams.builder()
                .model(ChatModel.of(model));
        for (ChatMessage m : messages) {
            params.addMessage(toParam(m));
        }
        return params.build();
    }

    private static ChatCompletionMessageParam toParam(ChatMessage message) {
        return switch (message.role()) {
            case SYSTEM -> ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(
                    ChatCompletionSystemMessageParam.builder()
                            .role(ChatCompletionSystemMessageParam.Role.SYSTEM)
                            .content(ChatCompletionSystemMessageParam.Content.ofTextContent(message.content()))
                            .build());
            case USER -> ChatCompletionMessageParam.ofChatCompletionUserMessageParam(
                    ChatCompletionUserMessageParam.builder()
                            .role(ChatCompletionUserMessageParam.Role.USER)
                            .content(ChatCompletionUserMessageParam.Content.ofTextContent(message.content()))
                            .build());
            case ASSISTANT -> ChatCompletionMessageParam.ofChatCompletionAssistantMessageParam(
                    ChatCompletionAssistantMessageParam.builder()
                            .role(ChatCompletionAssistantMessageParam.Role.ASSISTANT)
                            .content(ChatCompletionAssistantMessageParam.Content.ofTextContent(message.content()))
                            .build());
        };
    }
}
