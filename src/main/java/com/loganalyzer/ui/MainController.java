package com.loganalyzer.ui;

import com.loganalyzer.llm.ChatService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainController {

    private static final long STREAM_THRESHOLD_BYTES = 2L * 1024 * 1024;
    private static final int READ_CHUNK_CHARS = 64 * 1024;

    @FXML private TextArea logArea;
    @FXML private VBox chatMessages;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField chatInput;
    @FXML private Button sendButton;
    @FXML private MenuItem loadLogItem;
    @FXML private MenuItem configureItem;

    private Path loadedLogPath;
    private Runnable onConfigChanged;
    private ChatService chatService;

    @FXML
    public void initialize() {
    }

    private ChatService chatService() {
        if (chatService == null) {
            chatService = ChatService.fromConfig();
        }
        return chatService;
    }

    public void resetChatService() {
        chatService = null;
        chatMessages.getChildren().clear();
    }

    public void setOnConfigChanged(Runnable listener) {
        this.onConfigChanged = listener;
    }

    @FXML
    private void onLoadLog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Log");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Log files", "*.log", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        Window owner = logArea.getScene() != null ? logArea.getScene().getWindow() : null;
        java.io.File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        loadLogFile(file.toPath());
    }

    private void loadLogFile(Path path) {
        long size;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            showError("Could not read file: " + e.getMessage());
            return;
        }

        logArea.clear();
        sendButton.setDisable(true);
        loadLogItem.setDisable(true);

        Task<Void> task = (size > STREAM_THRESHOLD_BYTES) ? streamTask(path) : readAllTask(path);

        task.setOnSucceeded(e -> {
            loadedLogPath = path;
            updateTitle(path);
            sendButton.setDisable(false);
            loadLogItem.setDisable(false);
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showError("Failed to load log: " + (ex != null ? ex.getMessage() : "unknown error"));
            sendButton.setDisable(false);
            loadLogItem.setDisable(false);
        });

        Thread t = new Thread(task, "log-loader");
        t.setDaemon(true);
        t.start();
    }

    private Task<Void> readAllTask(Path path) {
        return new Task<>() {
            @Override
            protected Void call() throws IOException {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                Platform.runLater(() -> logArea.setText(content));
                return null;
            }
        };
    }

    private Task<Void> streamTask(Path path) {
        return new Task<>() {
            @Override
            protected Void call() throws IOException {
                char[] buf = new char[READ_CHUNK_CHARS];
                try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    int n;
                    while ((n = reader.read(buf)) != -1) {
                        if (isCancelled()) {
                            return null;
                        }
                        String chunk = new String(buf, 0, n);
                        Platform.runLater(() -> logArea.appendText(chunk));
                    }
                }
                return null;
            }
        };
    }

    private void updateTitle(Path path) {
        if (logArea.getScene() == null) {
            return;
        }
        Window window = logArea.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.setTitle("Log Analyzer — " + path.getFileName());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    Path getLoadedLogPath() {
        return loadedLogPath;
    }

    @FXML
    private void onConfigure() {
        ConfigDialog dialog = new ConfigDialog();
        Window owner = logArea.getScene() != null ? logArea.getScene().getWindow() : null;
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.showAndWait();
        if (dialog.wasApplied() && onConfigChanged != null) {
            onConfigChanged.run();
        }
    }

    @FXML
    private void onSend() {
        String text = chatInput.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        String userMessage = text.trim();
        String logContent = logArea.getText();

        appendBubble(userMessage, "user");
        chatInput.clear();
        sendButton.setDisable(true);
        chatInput.setDisable(true);

        MarkdownBubble assistantBubble = appendMarkdownBubble("assistant");

        ChatService service = chatService();
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return service.sendStreaming(userMessage, logContent, delta ->
                        Platform.runLater(() -> {
                            assistantBubble.append(delta);
                            chatScroll.setVvalue(1.0);
                        }));
            }
        };
        task.setOnSucceeded(e -> {
            assistantBubble.setText(task.getValue());
            sendButton.setDisable(false);
            chatInput.setDisable(false);
            chatInput.requestFocus();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            chatMessages.getChildren().remove(assistantBubble.getParent());
            appendBubble("Error: " + (ex != null ? ex.getMessage() : "unknown error"), "error");
            sendButton.setDisable(false);
            chatInput.setDisable(false);
            chatInput.requestFocus();
        });

        Thread t = new Thread(task, "chat-send");
        t.setDaemon(true);
        t.start();
    }

    private MarkdownBubble appendMarkdownBubble(String kind) {
        MarkdownBubble bubble = new MarkdownBubble(kind);
        HBox row = new HBox(bubble);
        row.getStyleClass().addAll("chat-row", "chat-row-" + kind);
        chatMessages.getChildren().add(row);
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
        return bubble;
    }

    private static final double BUBBLE_MAX_WIDTH = 520;
    private static final double BUBBLE_H_PADDING = 28;
    private static final double BUBBLE_V_PADDING = 18;

    private void appendBubble(String content, String kind) {
        TextArea bubble = new TextArea(content);
        bubble.setEditable(false);
        bubble.setWrapText(true);
        bubble.setFocusTraversable(false);
        bubble.setPrefRowCount(1);
        bubble.getStyleClass().addAll("chat-bubble", "chat-bubble-" + kind);

        bubble.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin == null) {
                return;
            }
            bubble.applyCss();
            bubble.layout();
            Node textNode = bubble.lookup(".text");
            if (!(textNode instanceof Text skinText)) {
                return;
            }
            Text measurer = new Text(content);
            measurer.setFont(skinText.getFont());
            double naturalW = measurer.getLayoutBounds().getWidth();
            double targetW = Math.min(naturalW + BUBBLE_H_PADDING, BUBBLE_MAX_WIDTH);
            measurer.setWrappingWidth(targetW - BUBBLE_H_PADDING);
            double wrappedH = measurer.getLayoutBounds().getHeight();
            bubble.setPrefWidth(targetW);
            bubble.setMaxWidth(targetW);
            bubble.setPrefHeight(wrappedH + BUBBLE_V_PADDING);
        });

        HBox row = new HBox(bubble);
        row.getStyleClass().addAll("chat-row", "chat-row-" + kind);

        chatMessages.getChildren().add(row);
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }
}
