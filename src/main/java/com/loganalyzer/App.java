package com.loganalyzer;

import com.loganalyzer.config.Config;
import com.loganalyzer.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void init() throws Exception {
        super.init();
        Config.load();
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.setOnConfigChanged(controller::resetChatService);

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(App.class.getResource("/styles.css").toExternalForm());
        stage.setTitle("Log Analyzer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
