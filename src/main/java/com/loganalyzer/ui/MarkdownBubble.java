package com.loganalyzer.ui;

import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;

public class MarkdownBubble extends StackPane {

    private static final List<org.commonmark.Extension> EXTENSIONS =
            List.of(TablesExtension.create());
    private static final Parser PARSER =
            Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER =
            HtmlRenderer.builder().extensions(EXTENSIONS).build();

    private static final double MAX_WIDTH = 520;

    private final WebView webView = new WebView();
    private final StringBuilder buffer = new StringBuilder();
    private boolean ready = false;
    private boolean pending = false;

    public MarkdownBubble(String bubbleKind) {
        getStyleClass().addAll("chat-bubble", "markdown-bubble", "chat-bubble-" + bubbleKind);
        setPadding(Insets.EMPTY);
        setStyle("-fx-padding: 0;");
        getChildren().add(webView);

        webView.setContextMenuEnabled(false);
        webView.setMinHeight(1);
        webView.setPrefHeight(1);
        webView.setMaxWidth(MAX_WIDTH);
        webView.setPrefWidth(MAX_WIDTH);
        webView.getEngine().setJavaScriptEnabled(true);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        setClip(clip);

        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldS, s) -> {
            if (s == Worker.State.SUCCEEDED) {
                ready = true;
                if (pending) {
                    pending = false;
                    render();
                }
            }
        });
        webView.getEngine().loadContent(shellHtml());
    }

    public void append(String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        buffer.append(delta);
        if (ready) {
            render();
        } else {
            pending = true;
        }
    }

    public void setText(String text) {
        buffer.setLength(0);
        buffer.append(text == null ? "" : text);
        if (ready) {
            render();
        } else {
            pending = true;
        }
    }

    private void render() {
        Node doc = PARSER.parse(buffer.toString());
        String html = RENDERER.render(doc);
        try {
            webView.getEngine().executeScript(
                    "document.getElementById('content').innerHTML = " + jsString(html) + ";");
            Object w = webView.getEngine().executeScript("document.body.scrollWidth");
            Object h = webView.getEngine().executeScript("document.body.scrollHeight");
            if (w instanceof Number wn && h instanceof Number hn) {
                double width = Math.min(wn.doubleValue() + 2, MAX_WIDTH);
                double height = hn.doubleValue() + 2;
                webView.setPrefWidth(width);
                webView.setMaxWidth(width);
                webView.setPrefHeight(height);
                webView.setMinHeight(height);
                setPrefWidth(width);
                setPrefHeight(height);
            }
        } catch (Exception ignored) {
        }
    }

    private static String jsString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private static String shellHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"><style>
                  html, body { margin: 0; padding: 0; }
                  html { background: #eceff4; }
                  body {
                    font-family: -apple-system, 'Segoe UI', Roboto, sans-serif;
                    font-size: 13px;
                    color: #1c1c1e;
                    background: #eceff4;
                    padding: 8px 12px;
                    width: fit-content;
                    max-width: 496px;
                    overflow: hidden;
                    word-wrap: break-word;
                  }
                  p { margin: 0.35em 0; }
                  p:first-child { margin-top: 0; }
                  p:last-child { margin-bottom: 0; }
                  code { font-family: Menlo, Consolas, monospace; background: rgba(0,0,0,0.06); padding: 1px 4px; border-radius: 3px; font-size: 12px; }
                  pre { background: rgba(0,0,0,0.06); padding: 6px 8px; border-radius: 4px; overflow-x: auto; margin: 0.4em 0; }
                  pre code { background: transparent; padding: 0; }
                  table { border-collapse: collapse; margin: 0.3em 0; }
                  th, td { border: 1px solid #bbb; padding: 3px 6px; text-align: left; }
                  ul, ol { margin: 0.3em 0; padding-left: 20px; }
                  h1, h2, h3, h4 { margin: 0.5em 0 0.2em; }
                  h1 { font-size: 16px; }
                  h2 { font-size: 15px; }
                  h3, h4 { font-size: 14px; }
                  a { color: #2f6feb; }
                  ::-webkit-scrollbar { display: none; }
                </style></head>
                <body><div id="content"></div></body>
                </html>
                """;
    }
}
