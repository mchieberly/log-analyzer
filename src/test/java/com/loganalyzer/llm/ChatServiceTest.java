package com.loganalyzer.llm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServiceTest {

    private static final String SYSTEM_PROMPT = "you analyze logs";
    private static final String MODEL = "test-model";
    private static final String LOG = "line 1\nline 2\nline 3";

    private static final class StubClient implements ChatCompletionClient {
        final List<List<ChatMessage>> captured = new ArrayList<>();
        final List<String> models = new ArrayList<>();
        String nextReply = "ok";

        @Override
        public String complete(List<ChatMessage> messages, String model) {
            captured.add(List.copyOf(messages));
            models.add(model);
            return nextReply;
        }
    }

    @Test
    void firstSendSeedsSystemAndLog() {
        StubClient stub = new StubClient();
        ChatService service = new ChatService(stub, MODEL, SYSTEM_PROMPT);

        stub.nextReply = "got it";
        String reply = service.send("what happened?", LOG);

        assertEquals("got it", reply);
        assertEquals(1, stub.captured.size());
        assertEquals(MODEL, stub.models.get(0));

        List<ChatMessage> sent = stub.captured.get(0);
        assertEquals(3, sent.size());

        assertEquals(ChatMessage.Role.SYSTEM, sent.get(0).role());
        assertEquals(SYSTEM_PROMPT, sent.get(0).content());

        assertEquals(ChatMessage.Role.USER, sent.get(1).role());
        String seeded = sent.get(1).content();
        assertTrue(seeded.startsWith("<log>\n"), "seeded log should be wrapped in <log> open tag");
        assertTrue(seeded.endsWith("\n</log>"), "seeded log should be wrapped in </log> close tag");
        assertTrue(seeded.contains(LOG), "seeded log should contain full log content");

        assertEquals(ChatMessage.Role.USER, sent.get(2).role());
        assertEquals("what happened?", sent.get(2).content());
    }

    @Test
    void secondSendAppendsWithoutReseeding() {
        StubClient stub = new StubClient();
        ChatService service = new ChatService(stub, MODEL, SYSTEM_PROMPT);

        stub.nextReply = "first";
        service.send("q1", LOG);
        stub.nextReply = "second";
        service.send("q2", LOG);

        assertEquals(2, stub.captured.size());

        List<ChatMessage> second = stub.captured.get(1);
        assertEquals(5, second.size());
        assertEquals(ChatMessage.Role.SYSTEM, second.get(0).role());
        assertEquals(ChatMessage.Role.USER, second.get(1).role());
        assertTrue(second.get(1).content().contains(LOG));
        assertEquals(ChatMessage.Role.USER, second.get(2).role());
        assertEquals("q1", second.get(2).content());
        assertEquals(ChatMessage.Role.ASSISTANT, second.get(3).role());
        assertEquals("first", second.get(3).content());
        assertEquals(ChatMessage.Role.USER, second.get(4).role());
        assertEquals("q2", second.get(4).content());
    }

    @Test
    void historyReflectsUserAndAssistantTurns() {
        StubClient stub = new StubClient();
        ChatService service = new ChatService(stub, MODEL, SYSTEM_PROMPT);

        stub.nextReply = "reply-a";
        service.send("hello", LOG);

        List<ChatMessage> history = service.history();
        assertEquals(4, history.size());
        assertEquals(ChatMessage.Role.SYSTEM, history.get(0).role());
        assertEquals(ChatMessage.Role.USER, history.get(1).role());
        assertEquals(ChatMessage.Role.USER, history.get(2).role());
        assertEquals("hello", history.get(2).content());
        assertEquals(ChatMessage.Role.ASSISTANT, history.get(3).role());
        assertEquals("reply-a", history.get(3).content());
    }

    @Test
    void resetClearsHistoryAndNextSendReSeeds() {
        StubClient stub = new StubClient();
        ChatService service = new ChatService(stub, MODEL, SYSTEM_PROMPT);

        service.send("q1", LOG);
        service.reset();
        assertTrue(service.history().isEmpty());

        service.send("q2", "different log");
        List<ChatMessage> afterReset = stub.captured.get(1);
        assertEquals(3, afterReset.size());
        assertEquals(ChatMessage.Role.SYSTEM, afterReset.get(0).role());
        assertTrue(afterReset.get(1).content().contains("different log"));
        assertEquals("q2", afterReset.get(2).content());
    }
}
