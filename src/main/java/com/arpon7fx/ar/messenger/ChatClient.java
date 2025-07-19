package com.arpon7fx.ar.messenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private MessageListener messageListener;
    private final String username;
    private Thread messageListenerThread;
    private long lastMessageTime;
    
    public interface MessageListener {
        void onMessageReceived(String message);
        void onConnectionStatusChanged(boolean connected);
        void onError(String error);
        void onUserListUpdated(String[] users);
    }
    
    public ChatClient(String username) {
        this.username = username;
        this.lastMessageTime = System.currentTimeMillis();
    }
    
    public boolean connect(String serverHost, int serverPort) {
        return connect(serverHost, serverPort, 5000); // 5 second timeout
    }
    
    public boolean connect(String serverHost, int serverPort, int timeoutMs) {
        try {
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(serverHost, serverPort), timeoutMs);
            socket.setSoTimeout(30000); // 30 second read timeout
            
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            connected.set(true);
            
            // Send username as first message
            writer.println(username);
            writer.flush();
            
            // Start listening for incoming messages
            messageListenerThread = new Thread(this::listenForMessages, "MessageListener-" + username);
            messageListenerThread.setDaemon(true);
            messageListenerThread.start();
            
            // Start keepalive thread
            startKeepalive();
            
            if (messageListener != null) {
                messageListener.onConnectionStatusChanged(true);
            }
            
            System.out.println("Connected to server as " + username);
            return true;
            
        } catch (IOException e) {
            String errorMsg = "Failed to connect to server at " + serverHost + ":" + serverPort + " - " + e.getMessage();
            System.err.println(errorMsg);
            connected.set(false);
            
            if (messageListener != null) {
                messageListener.onConnectionStatusChanged(false);
                messageListener.onError(errorMsg);
            }
            return false;
        }
    }
    
    private void listenForMessages() {
        try {
            String message;
            while (connected.get() && (message = reader.readLine()) != null) {
                lastMessageTime = System.currentTimeMillis();
                
                if (messageListener != null) {
                    handleIncomingMessage(message);
                }
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Socket timeout - server may be unreachable");
            if (messageListener != null) {
                messageListener.onError("Connection timeout - server may be unreachable");
            }
        } catch (SocketException e) {
            if (connected.get()) {
                System.err.println("Connection lost: " + e.getMessage());
                if (messageListener != null) {
                    messageListener.onError("Connection lost");
                }
            }
        } catch (IOException e) {
            if (connected.get()) {
                System.err.println("Error reading messages: " + e.getMessage());
                if (messageListener != null) {
                    messageListener.onError("Error reading messages: " + e.getMessage());
                }
            }
        } finally {
            disconnect();
        }
    }
    
    private void handleIncomingMessage(String message) {
        if (message.startsWith("ERROR:")) {
            messageListener.onError(message.substring(6));
        } else if (message.startsWith("USERS:")) {
            String userList = message.substring(6);
            if (!userList.isEmpty()) {
                String[] users = userList.split(",");
                messageListener.onUserListUpdated(users);
            }
        } else if (message.startsWith("PRIVATE:")) {
            messageListener.onMessageReceived(message.substring(8));
        } else {
            messageListener.onMessageReceived(message);
        }
    }
    
    private void startKeepalive() {
        Thread keepaliveThread = new Thread(() -> {
            while (connected.get()) {
                try {
                    Thread.sleep(10000); // Send keepalive every 10 seconds
                    if (connected.get()) {
                        sendMessage("/ping");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Keepalive-" + username);
        keepaliveThread.setDaemon(true);
        keepaliveThread.start();
    }
    
    public boolean sendMessage(String message) {
        if (connected.get() && writer != null && !message.trim().isEmpty()) {
            try {
                writer.println(message.trim());
                writer.flush();
                return true;
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
                if (messageListener != null) {
                    messageListener.onError("Failed to send message");
                }
                return false;
            }
        }
        return false;
    }
    
    public void sendPrivateMessage(String targetUser, String message) {
        sendMessage("/whisper " + targetUser + " " + message);
    }
    
    public void requestUserList() {
        sendMessage("/users");
    }
    
    public void disconnect() {
        if (connected.getAndSet(false)) {
            try {
                // Interrupt the message listener thread
                if (messageListenerThread != null) {
                    messageListenerThread.interrupt();
                }
                
                if (socket != null) socket.close();
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                
            } catch (IOException e) {
                System.err.println("Error disconnecting: " + e.getMessage());
            }
            
            if (messageListener != null) {
                messageListener.onConnectionStatusChanged(false);
            }
            
            System.out.println("Disconnected from server");
        }
    }
    
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed() && socket.isConnected();
    }
    
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }
    
    public String getUsername() {
        return username;
    }
    
    public long getLastMessageTime() {
        return lastMessageTime;
    }
    
    public String getServerAddress() {
        if (socket != null && socket.isConnected()) {
            return socket.getRemoteSocketAddress().toString();
        }
        return "Not connected";
    }
    
    // Test connection to server without connecting
    public static boolean testConnection(String serverHost, int serverPort) {
        try (Socket testSocket = new Socket()) {
            testSocket.connect(new java.net.InetSocketAddress(serverHost, serverPort), 3000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
