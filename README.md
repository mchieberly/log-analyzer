# Log Analyzer

A JavaFX desktop application for analyzing log files with the help of an LLM. Load a log file, view and edit it in-app, then chat with an AI assistant that has full context of the log to help you diagnose issues, spot patterns, and understand what happened.

Works with any OpenAI API-compatible endpoint (OpenAI, local models via Ollama/LM Studio, etc.).

## Features

- **Log Viewer** — Load and browse log files in a scrollable, editable text pane.
- **AI Chat** — Ask questions about the loaded log using a configurable LLM. The full log content is included as context alongside your messages.
- **Configurable** — Choose your model, API endpoint, and system prompt via an in-app configuration dialog. Settings are persisted in XML.

## Prerequisites

- Java 21+
- A running OpenAI API-compatible server (OpenAI, Ollama, LM Studio, etc.)

## Building & Running

The project uses the Gradle wrapper, so no global Gradle installation is needed.

```bash
# Build
./gradlew build

# Run
./gradlew run
```

## Usage

1. **Load a log file** — `File > Load Log` to open a log file into the left-side viewer.
2. **Configure the AI** — `File > Configure` to set:
   - API endpoint URL
   - Model name
   - System prompt (instructions for how the AI should analyze logs)
3. **Chat** — Type a question in the right-side chat panel. The AI receives your system prompt, the full log file, and your message, then responds in the chat.

## Design

### Tech Stack

| Component     | Technology                                                     |
|---------------|----------------------------------------------------------------|
| Language      | Java                                                           |
| UI Framework  | JavaFX                                                         |
| Build Tool    | Gradle 8.7 (wrapper included)                                  |
| LLM Client    | [openai-java](https://github.com/openai/openai-java)          |
| Testing       | JUnit 5                                                        |
| Configuration | XML                                                            |

### UI Layout

```
┌──────────────────────────────────────────────────┐
│  Menu Bar  [Load Log]  [Configure]               │
├────────────────────────┬─────────────────────────┤
│                        │                         │
│   Log Viewer           │   Chat Panel            │
│   (editable text pane) │                         │
│                        │   [AI responses]        │
│                        │   [User messages]       │
│                        │                         │
│                        │   [Input] [Send]        │
├────────────────────────┴─────────────────────────┤
└──────────────────────────────────────────────────┘
```

- **Left Panel** — Scrollable, editable text pane displaying the loaded log file.
- **Right Panel** — Chat interface. On the first message, the configured system prompt and full log content are sent along with the user's question. Follow-up messages continue the conversation with the same context.

### Configuration

Settings are stored in XML and include:

- **API Base URL** — The endpoint for the OpenAI-compatible API.
- **Model** — The model identifier to use (e.g. `gpt-4o`, `llama3`, a local model name).
- **System Prompt** — Instructions that guide how the AI interprets and analyzes the log file.
