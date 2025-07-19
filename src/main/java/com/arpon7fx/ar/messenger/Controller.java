package com.arpon7fx.ar.messenger;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Controller implements Initializable, ChatClient.MessageListener {
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField serverField;
    
    @FXML
    private Button connectButton;
    
    @FXML
    private Button hostButton;
    
    @FXML
    private Button disconnectButton;
    
    @FXML
    private ScrollPane messagesScrollPane;
    
    @FXML
    private VBox messagesVBox;
    
    @FXML
    private TextField messageTextField;
    
    @FXML
    private Button sendButton;
    
    private ChatClient chatClient;
    private ChatServer chatServer;
    private boolean isHost = false;
    private Thread serverThread;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize UI state
        updateConnectionState(false);
        
        // Auto-scroll to bottom when new messages are added
        messagesVBox.heightProperty().addListener((observable, oldValue, newValue) -> {
            messagesScrollPane.setVvalue(1.0);
        });
        
        // Enable send on Enter key
        messageTextField.setOnAction(e -> onSendMessageClick());
        
        // Add welcome message
        addSystemMessage("Welcome to Arpon's Messenger! Enter your username and connect to start chatting.");
    }
    
    @FXML
    protected void onConnectClick() {
        String username = usernameField.getText().trim();
        String server = serverField.getText().trim();
        
        if (username.isEmpty()) {
            showAlert("Error", "Please enter a username");
            return;
        }
        
        if (server.isEmpty()) {
            server = "localhost";
        }
        
        // Test connection first
        if (!ChatClient.testConnection(server, 12345)) {
            showAlert("Connection Error", "Cannot reach server at " + server + ":12345");
            return;
        }
        
        chatClient = new ChatClient(username);
        chatClient.setMessageListener(this);
        
        if (chatClient.connect(server, 12345)) {
            addSystemMessage("Connected to server as " + username);
        } else {
            showAlert("Connection Error", "Failed to connect to server at " + server);
        }
    }
    
    @FXML
    protected void onHostClick() {
        String username = usernameField.getText().trim();
        
        if (username.isEmpty()) {
            showAlert("Error", "Please enter a username");
            return;
        }
        
        // Start server in background thread
        serverThread = new Thread(() -> {
            chatServer = new ChatServer();
            Platform.runLater(() -> {
                addSystemMessage("Server started on port 12345");
                statusLabel.setText("Hosting");
                statusLabel.setTextFill(Color.web("#f39c12"));
                isHost = true;
                updateConnectionState(true);
                
                // Auto-connect to own server after a short delay
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(1000); // Wait for server to fully start
                        chatClient = new ChatClient(username);
                        chatClient.setMessageListener(this);
                        if (chatClient.connect("localhost", 12345)) {
                            addSystemMessage("Connected to your own server as " + username);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            });
            chatServer.start();
        }, "ServerThread");
        
        serverThread.setDaemon(true);
        serverThread.start();
    }
    
    @FXML
    protected void onDisconnectClick() {
        if (chatClient != null) {
            chatClient.disconnect();
            chatClient = null;
        }
        if (chatServer != null && isHost) {
            chatServer.stop();
            chatServer = null;
            isHost = false;
        }
        addSystemMessage("Disconnected from chat");
    }
    
    @FXML
    protected void onSendMessageClick() {
        String message = messageTextField.getText().trim();
        if (!message.isEmpty() && chatClient != null && chatClient.isConnected()) {
            // Don't add message to our own chat here - let the server echo it back
            // This ensures proper message ordering
            if (chatClient.sendMessage(message)) {
                messageTextField.clear();
            }
        }
    }
    
    @Override
    public void onMessageReceived(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("SYSTEM:")) {
                addSystemMessage(message.substring(7));
            } else {
                String[] parts = message.split(":", 2);
                if (parts.length == 2) {
                    String sender = parts[0];
                    String content = parts[1];
                    boolean isCurrentUser = chatClient != null && sender.equals(chatClient.getUsername());
                    addMessage(sender, content, isCurrentUser);
                }
            }
        });
    }
    
    @Override
    public void onConnectionStatusChanged(boolean connected) {
        Platform.runLater(() -> updateConnectionState(connected));
    }
    
    @Override
    public void onError(String error) {
        Platform.runLater(() -> {
            addSystemMessage("Error: " + error);
            showAlert("Connection Error", error);
        });
    }
    
    @Override
    public void onUserListUpdated(String[] users) {
        Platform.runLater(() -> {
            if (users.length > 0) {
                addSystemMessage("Online users: " + String.join(", ", users));
            }
        });
    }
    
    private void updateConnectionState(boolean connected) {
        connectButton.setDisable(connected);
        hostButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        usernameField.setDisable(connected);
        serverField.setDisable(connected);
        messageTextField.setDisable(!connected);
        sendButton.setDisable(!connected);
        
        if (connected && !isHost) {
            statusLabel.setText("Connected");
            statusLabel.setTextFill(Color.web("#27ae60"));
        } else if (!connected && !isHost) {
            statusLabel.setText("Disconnected");
            statusLabel.setTextFill(Color.web("#e74c3c"));
        }
        // If hosting, status is set in onHostClick
    }
    
    private void addMessage(String sender, String message, boolean isCurrentUser) {
        HBox messageContainer = new HBox();
        messageContainer.setSpacing(10);
        messageContainer.setPadding(new Insets(5, 10, 5, 10));
        
        VBox messageBox = new VBox();
        messageBox.setSpacing(2);
        messageBox.setMaxWidth(500);
        
        // Sender label
        Label senderLabel = new Label(sender);
        senderLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        senderLabel.setTextFill(isCurrentUser ? Color.web("#2c3e50") : Color.web("#27ae60"));
        
        // Message content
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setFont(Font.font("System", 14));
        messageLabel.setStyle(isCurrentUser ? 
            "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8; -fx-background-radius: 15;" :
            "-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-padding: 8; -fx-background-radius: 15;");
        
        // Timestamp
        Label timeLabel = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.setFont(Font.font("System", 10));
        timeLabel.setTextFill(Color.GRAY);
        
        messageBox.getChildren().addAll(senderLabel, messageLabel, timeLabel);
        
        if (isCurrentUser) {
            messageContainer.setAlignment(Pos.CENTER_RIGHT);
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageContainer.setAlignment(Pos.CENTER_LEFT);
            messageBox.setAlignment(Pos.CENTER_LEFT);
        }
        
        messageContainer.getChildren().add(messageBox);
        messagesVBox.getChildren().add(messageContainer);
    }
    
    private void addSystemMessage(String message) {
        HBox systemContainer = new HBox();
        systemContainer.setAlignment(Pos.CENTER);
        systemContainer.setPadding(new Insets(10));
        
        Label systemLabel = new Label(message);
        systemLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        systemLabel.setTextFill(Color.GRAY);
        systemLabel.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8; -fx-background-radius: 10;");
        
        systemContainer.getChildren().add(systemLabel);
        messagesVBox.getChildren().add(systemContainer);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Called when application is closing
    public void cleanup() {
        if (chatClient != null) {
            chatClient.disconnect();
        }
        if (chatServer != null && isHost) {
            chatServer.stop();
        }
    }
}
