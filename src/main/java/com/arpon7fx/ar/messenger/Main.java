package com.arpon7fx.ar.messenger;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    
    private static Controller controller;
    
    @Override
    public void start(Stage stage) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("hello-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            
            // Get the controller instance for cleanup
            controller = fxmlLoader.getController();
            
            // Configure the stage
            stage.setTitle("Arpon's Messenger - Real-time Chat");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            
            // Set application icon (if available)
            try {
                stage.getIcons().add(new Image(Main.class.getResourceAsStream("/icon.png")));
            } catch (Exception e) {
                // Icon not found, continue without it
            }
            
            // Handle window close event
            stage.setOnCloseRequest(event -> {
                // Perform cleanup before closing
                if (controller != null) {
                    controller.cleanup();
                }
                
                // Close any running servers
                Platform.exit();
                System.exit(0);
            });
            
            stage.show();
            
            // Show welcome dialog
            showWelcomeDialog();
            
        } catch (IOException e) {
            showErrorDialog("Application Error", "Failed to load the application: " + e.getMessage());
            Platform.exit();
        }
    }
    
    private void showWelcomeDialog() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Welcome to Arpon's Messenger");
            alert.setHeaderText("Real-time Local Chat Application");
            alert.setContentText("""
                Welcome to Arpon's Messenger!

                To get started:
                1. Enter your username
                2. Click 'Host' to start a server, or
                3. Enter server address and click 'Connect'

                Features:
                • Real-time messaging
                • Private messages (/whisper <user> <message>)
                • User list (/users)
                • Server ping (/ping)

                Enjoy chatting!"""
            );
            alert.showAndWait();
        });
    }
    
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public void stop() throws Exception {
        // Cleanup when application is stopping
        if (controller != null) {
            controller.cleanup();
        }
        super.stop();
    }
    
    public static void main(String[] args) {
        // Handle uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            System.err.println("Uncaught exception in thread " + thread.getName());
            System.err.println("Exception: " + exception.getMessage());
            
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Unexpected Error");
                alert.setHeaderText("An unexpected error occurred");
                alert.setContentText("Error: " + exception.getMessage() + 
                    "\n\nThe application may need to be restarted.");
                alert.showAndWait();
            });
        });
        
        launch(args);
    }
}