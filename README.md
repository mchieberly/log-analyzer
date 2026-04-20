# Log Analyzer

A JavaFX desktop app for analyzing log files with an LLM assistant. Load a log, view or edit it in-app, then chat with an AI that has the full log as context.

Works with any OpenAI API-compatible endpoint (OpenAI, Ollama, LM Studio, etc.).

## Prerequisites

- Java 21+
- A reachable OpenAI API-compatible server

## Build & Run

Uses the Gradle wrapper — no global Gradle install needed.

```bash
./gradlew build    # compile + run tests
./gradlew test     # tests only
./gradlew run      # launch the app
```

## Usage

1. **Configure the LLM** — `File > Configure` and set:
   - **API base URL** (e.g. `https://api.openai.com/v1`, `http://localhost:11434/v1`)
   - **API key** (leave blank for local endpoints that don't require one)
   - **Model** (e.g. `gpt-4o-mini`, `llama3`)
   - **System prompt** — instructions that guide how the AI reads the log

   Settings persist to `~/.loganalyzer/config.xml`. Saving resets the current chat.

2. **Load a log** — `File > Load Log` opens the file into the left pane. Large files stream in on a background thread; the Send button stays disabled until the load finishes. The window title updates to show the filename.

3. **Chat** — Type a question in the right pane and press Enter or click **Send**. The first message seeds the conversation with your system prompt and the full log; each reply appears as an assistant bubble and the view auto-scrolls. Errors surface inline as error bubbles.

## UI Layout

```
┌──────────────────────────────────────────────────┐
│  File  [Load Log]  [Configure]                   │
├────────────────────────┬─────────────────────────┤
│                        │                         │
│   Log viewer           │   Chat                  │
│   (editable)           │                         │
│                        │   [assistant bubbles]   │
│                        │   [user bubbles]        │
│                        │                         │
│                        │   [Input.........] Send │
└────────────────────────┴─────────────────────────┘
```

## Tech Stack

| Component     | Technology                                           |
|---------------|------------------------------------------------------|
| Language      | Java 21                                              |
| UI            | JavaFX 21.0.2                                        |
| Build         | Gradle 8.7 (wrapper)                                 |
| LLM client    | [openai-java](https://github.com/openai/openai-java) |
| Tests         | JUnit 5                                              |
| Config format | XML (`~/.loganalyzer/config.xml`)                    |
