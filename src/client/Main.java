package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("resources/connect.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("Black Jack");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.centerOnScreen();

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    @FXML
    private void handleQuit() {
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void connect() {
        System.out.println("Connecting...");
//        Each player needs to create a server socket and a client socket?
    }

}

