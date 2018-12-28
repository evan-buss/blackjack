/*
 * Author: Evan Buss
 * Major: Computer Science
 * Creation Date: November 06, 2018
 * Due Date: December 05, 2018
 * Course: CSC328 - 020 Network Programming
 * Professor: Dr. Frye
 * Assignment: Download Client / Server
 * Filename: Server.java
 * Purpose:  Server daemon that waits for client connections.
 *           When a connection is received, the server creates
 *           a new thread to handle it. The client sends
 *           requests to navigate the server's directories and
 *           download files. The server sends these files over
 *           a TCP connection.
 * Language: Java 8 (1.8.0_101)
 * Compilation Command: javac Server.java
 * Execution Command: java Server [port]
 * Shutting Down: Ctrl+C
 */

/*
 * Modified version of previously written download server code...
 */

package server;


import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;


public class Main extends Application {
    List<String> args;
    @FXML
    static TextField logArea;

    static  TextField getLogArea() {
        return logArea;
    }


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        args = getParameters().getRaw();

        primaryStage.setTitle("Black Jack Server Console");
        primaryStage.centerOnScreen();

        Parent root = FXMLLoader.load(getClass().getResource("resources/Server.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setScene(scene);

        primaryStage.show();
    }

    @FXML
    public void startServer() {

//        Create new thread to handle the server daemon. It always waits
//        and blocks so it can't be the same thread as UI
        new Thread(new ServerDaemon()).start();
    }

    @FXML
    public void stopServer() {
        System.out.println("Stopping server!");
    }
}
