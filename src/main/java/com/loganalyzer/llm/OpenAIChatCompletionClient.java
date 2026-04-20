package com.loganalyzer.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionAssistantMessageParam;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;
import com.openai.models.ChatModel;

import java.util.List;

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
        ChatCompletionCreateParams.Builder params = ChatCompletionCreateParams.builder()
                .model(ChatModel.of(model));
        for (ChatMessage m : messages) {
            params.addMessage(toParam(m));
        }
        ChatCompletion completion = client.chat().completions().create(params.build());
        return completion.choices().stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElse("");
    }

    private static ChatCompletionMessageParam toParam(ChatMessage message) {
        return switch (message.role()) {
            case SYSTEM -> ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(
                    ChatCompletionSystemMessageParam.builder()
                            .content(ChatCompletionSystemMessageParam.Content.ofTextContent(message.content()))
                            .build());
            case USER -> ChatCompletionMessageParam.ofChatCompletionUserMessageParam(
                    ChatCompletionUserMessageParam.builder()
                            .content(ChatCompletionUserMessageParam.Content.ofTextContent(message.content()))
                            .build());
            case ASSISTANT -> ChatCompletionMessageParam.ofChatCompletionAssistantMessageParam(
                    ChatCompletionAssistantMessageParam.builder()
                            .content(ChatCompletionAssistantMessageParam.Content.ofTextContent(message.content()))
                            .build());
        };
    }
}
