package com.loganalyzer.ui;

import com.loganalyzer.config.Config;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class ConfigDialog extends Dialog<Void> {

    private final TextField apiBaseUrlField = new TextField();
    private final PasswordField apiKeyField = new PasswordField();
    private final TextField modelField = new TextField();
    private final TextArea systemPromptArea = new TextArea();

    private boolean applied;

    public ConfigDialog() {
        setTitle("Configure");
        setHeaderText("LLM connection and prompt settings");

        Config cfg = Config.getInstance();
        apiBaseUrlField.setText(cfg.getApiBaseUrl());
        apiKeyField.setText(cfg.getApiKey());
        modelField.setText(cfg.getModel());
        systemPromptArea.setText(cfg.getSystemPrompt());
        systemPromptArea.setWrapText(true);
        systemPromptArea.setPrefRowCount(6);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        grid.add(new Label("API base URL:"), 0, 0);
        grid.add(apiBaseUrlField, 1, 0);
        grid.add(new Label("API key:"), 0, 1);
        grid.add(apiKeyField, 1, 1);
        grid.add(new Label("Model:"), 0, 2);
        grid.add(modelField, 1, 2);
        grid.add(new Label("System prompt:"), 0, 3);
        grid.add(systemPromptArea, 1, 3);

        apiBaseUrlField.setPrefColumnCount(40);
        GridPane.setHgrow(apiBaseUrlField, Priority.ALWAYS);
        GridPane.setHgrow(apiKeyField, Priority.ALWAYS);
        GridPane.setHgrow(modelField, Priority.ALWAYS);
        GridPane.setHgrow(systemPromptArea, Priority.ALWAYS);
        GridPane.setVgrow(systemPromptArea, Priority.ALWAYS);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                Config c = Config.getInstance();
                c.setApiBaseUrl(apiBaseUrlField.getText().trim());
                c.setApiKey(apiKeyField.getText());
                c.setModel(modelField.getText().trim());
                c.setSystemPrompt(systemPromptArea.getText());
                c.save();
                applied = true;
            }
            return null;
        });
    }

    public boolean wasApplied() {
        return applied;
    }
}
