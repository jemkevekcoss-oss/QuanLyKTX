package com.ptit.quanlyktx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import java.net.URL;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader =   new FXMLLoader(App.class.getResource("/com/ptit/quanlyktx/fxml/MainView.fxml"));
        Scene scene = new Scene(loader.load(), 1150, 720);
        scene.getStylesheets().add(App.class.getResource("/com/ptit/quanlyktx/css/style.css").toExternalForm());
        stage.setTitle("PTIT KTX | Quản lý ký túc xá");
        // Taskbar/window icon; missing resource must never prevent app startup.
        URL logoUrl = App.class.getResource("/com/ptit/quanlyktx/images/ptit_logo_app.png");
        if (logoUrl != null) {
            stage.getIcons().add(new Image(logoUrl.toExternalForm()));
        }
        stage.setMinWidth(1400);
        stage.setMinHeight(1000);
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}

// clean javafx:run
