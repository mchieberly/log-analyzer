# CLAUDE.md

Guidance for Claude Code working in this repo.

## Project

A JavaFX desktop app for analyzing log files with an LLM assistant. See `README.md` for the user-facing feature/UX description. Build config is in `build.gradle` (Java 21, JavaFX 21.0.2, `openai-java` 0.8.1, JUnit 5). Main class is `com.loganalyzer.App`.

## Source Layout

Gradle's default Java plugin layout:

- Source: `src/main/java/com/loganalyzer/`
- Resources: `src/main/resources/` (FXML, CSS, icons)
- Tests: `src/test/java/com/loganalyzer/`

## Architecture

Package root `com.loganalyzer`, sub-packages `ui`, `config`, `llm`.

- **`App`** — JavaFX `Application` entry point. `init()` triggers `Config.load()`; `start()` loads `main.fxml` via an `FXMLLoader` instance so it can grab the `MainController` and wire `setOnConfigChanged(controller::resetChatService)`. Attaches `styles.css` to the scene.
- **`config.Config`** — singleton holding `apiBaseUrl`/`apiKey`/`model`/`systemPrompt` with defaults. Reads/writes `~/.loganalyzer/config.xml` via `XMLEncoder`/`XMLDecoder` using `config.ConfigBean` as the persistence DTO. `setConfigPathForTesting` / `resetConfigPathForTesting` let tests point at a temp dir. Only the Configure dialog mutates the singleton.
- **`ui.MainController`** — FXML controller. Handles `Load Log` (FileChooser → background `Task` that either `Files.readString` or streams in 64 KB chunks above 2 MB), `Configure` (opens `ConfigDialog`, fires `onConfigChanged` listener on OK), and `Send` (appends user bubble, runs `ChatService.send` on a `Task`, appends assistant/error bubble on completion, scrolls `chatScroll` to bottom). Lazily constructs the `ChatService` via `ChatService.fromConfig()`; `resetChatService()` nulls it and clears `chatMessages`.
- **`ui.ConfigDialog`** — `Dialog<Void>` with `TextField`/`PasswordField`/`TextField`/`TextArea` for base URL / API key / model / system prompt. Pre-fills from `Config.getInstance()`; on OK the result converter writes the fields back and calls `save()`, then flags `applied = true`. Caller checks `wasApplied()`.
- **`llm.ChatService`** — holds `List<ChatMessage>` history per session. First `send()` seeds `SYSTEM` (configured prompt) + a `USER` turn wrapping the log in `<log>…</log>`. Each send appends the user message, delegates to `ChatCompletionClient.complete`, appends the reply, returns it. `reset()` clears history. Methods are `synchronized` so background `Task`s can call safely.
- **`llm.ChatCompletionClient`** — narrow interface `complete(List<ChatMessage>, String model) → String`. Lets `ChatServiceTest` stub without touching the network.
- **`llm.OpenAIChatCompletionClient`** — default impl wrapping `OpenAIOkHttpClient`. Translates `ChatMessage` to the per-role `ChatCompletion*MessageParam` union and extracts the first choice's content.
- **Resources** — `main.fxml` (BorderPane → MenuBar + SplitPane with log `TextArea` / chat `VBox` + input), `styles.css` (`chat-row-*` / `chat-bubble-*` classes for left/right aligned bubbles).
- **Tests** — `ConfigTest` (defaults, save creates parent dirs, round-trip through a fresh singleton against a `@TempDir`). `ChatServiceTest` (stub client verifies seeding, no re-seed on subsequent sends, history contents, and reset behavior).

## Conventions

- Controllers stay thin — business logic lives in `config` / `llm`.
- All network and file I/O runs off the FX thread (via `Task`).
- Read config only through `Config.getInstance()`; never parse `config.xml` from elsewhere.
- Don't log the API key. Don't commit config files containing secrets (`~/.loganalyzer/config.xml` is fine, nothing similar in-repo).

## Commands

```bash
./gradlew build    # compile + test
./gradlew test     # tests only
./gradlew run      # launch the app
```

## Manual verification (remaining)

Code for all planned features is in place. End-to-end verification against a real LLM endpoint is the only outstanding item: `./gradlew run`, load a small log, open Configure, point at a reachable OpenAI-compatible endpoint, send a message, confirm the response renders and the chat auto-scrolls.
