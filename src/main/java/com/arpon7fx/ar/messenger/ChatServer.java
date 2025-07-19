package com.arpon7fx.ar.messenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private ChatServer server;
    private String username;
    private boolean connected;
    private long lastActivity;
    
    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.connected = true;
        this.lastActivity = System.currentTimeMillis();
        
        try {
            socket.setSoTimeout(30000); // 30 second timeout
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error creating client handler: " + e.getMessage());
            connected = false;
        }
    }
    
    @Override
    public void run() {
        try {
            // First message should be the username
            username = reader.readLine();
            if (username != null && !username.trim().isEmpty()) {
                username = username.trim();
                updateActivity();
                
                // Check if username is already taken
                if (server.isUsernameTaken(username)) {
                    sendMessage("ERROR:Username already taken");
                    disconnect();
                    return;
                }
                
                server.addActiveUser(username);
                server.broadcastMessage("SYSTEM:" + username + " joined the chat", this);
                server.broadcastUserList();
                
                String message;
                while (connected && (message = reader.readLine()) != null) {
                    updateActivity();
                    message = message.trim();
                    
                    if (!message.isEmpty()) {
                        // Handle special commands
                        if (message.startsWith("/")) {
                            handleCommand(message);
                        } else {
                            // Regular message - broadcast to all other clients
                            server.broadcastMessage(username + ":" + message, this);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            // Client disconnected normally
            System.out.println("Client " + (username != null ? username : "unknown") + " disconnected");
        } catch (IOException e) {
            if (connected) {
                System.err.println("Client handler error for " + 
                    (username != null ? username : "unknown") + ": " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }
    
    private void handleCommand(String command) {
        if (command.equals("/ping")) {
            sendMessage("SYSTEM:Pong! Server is alive.");
        } else if (command.equals("/users")) {
            sendMessage("SYSTEM:Online users: " + server.getActiveUsers());
        } else if (command.equals("/time")) {
            sendMessage("SYSTEM:Server time: " + new java.util.Date());
        } else if (command.startsWith("/whisper ")) {
            handleWhisperCommand(command);
        } else {
            sendMessage("SYSTEM:Unknown command. Available: /ping, /users, /time, /whisper <user> <message>");
        }
    }
    
    private void handleWhisperCommand(String command) {
        String[] parts = command.split(" ", 3);
        if (parts.length >= 3) {
            String targetUser = parts[1];
            String message = parts[2];
            if (server.sendPrivateMessage(targetUser, username + " (whisper): " + message)) {
                sendMessage("SYSTEM:Whisper sent to " + targetUser);
            } else {
                sendMessage("SYSTEM:User " + targetUser + " not found");
            }
        } else {
            sendMessage("SYSTEM:Usage: /whisper <username> <message>");
        }
    }
    
    private void updateActivity() {
        lastActivity = System.currentTimeMillis();
    }
    
    public void sendMessage(String message) {
        if (writer != null && connected) {
            writer.println(message);
            writer.flush();
        }
    }
    
    public void disconnect() {
        if (connected) {
            connected = false;
            
            try {
                if (username != null) {
                    server.removeActiveUser(username);
                    server.broadcastMessage("SYSTEM:" + username + " left the chat", this);
                    server.broadcastUserList();
                }
                if (socket != null) socket.close();
                if (reader != null) reader.close();
                if (writer != null) writer.close();
            } catch (IOException e) {
                System.err.println("Error disconnecting client " + 
                    (username != null ? username : "unknown") + ": " + e.getMessage());
            }
            
            server.removeClient(this);
        }
    }
    
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
    
    public String getUsername() {
        return username;
    }
    
    public long getLastActivity() {
        return lastActivity;
    }
}

public class ChatServer {
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients;
    private final List<String> activeUsers;
    private boolean isRunning;
    private static final int PORT = 12345;
    private static final int MAX_CLIENTS = 50;
    
    public ChatServer() {
        clients = new CopyOnWriteArrayList<>();
        activeUsers = Collections.synchronizedList(new ArrayList<>());
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            System.out.println("Chat server started on port " + PORT);
            System.out.println("Maximum clients: " + MAX_CLIENTS);
            
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    if (clients.size() >= MAX_CLIENTS) {
                        // Reject connection - server full
                        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                        writer.println("ERROR:Server is full. Try again later.");
                        clientSocket.close();
                        continue;
                    }
                    
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clients.add(clientHandler);
                    new Thread(clientHandler, "Client-" + clientSocket.getRemoteSocketAddress()).start();
                    System.out.println("New client connected from " + 
                        clientSocket.getRemoteSocketAddress() + ". Total clients: " + clients.size());
                        
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    void broadcastMessage(String message, ClientHandler sender) {
        System.out.println("Broadcasting: " + message);
        for (ClientHandler client : clients) {
            if (client != sender && client.isConnected()) {
                client.sendMessage(message);
            }
        }
    }
    
    void broadcastUserList() {
        String userList = "USERS:" + String.join(",", activeUsers);
        for (ClientHandler client : clients) {
            if (client.isConnected()) {
                client.sendMessage(userList);
            }
        }
    }
    
    boolean sendPrivateMessage(String targetUsername, String message) {
        for (ClientHandler client : clients) {
            if (client.isConnected() && targetUsername.equals(client.getUsername())) {
                client.sendMessage("PRIVATE:" + message);
                return true;
            }
        }
        return false;
    }
    
    boolean isUsernameTaken(String username) {
        return activeUsers.contains(username);
    }
    
    void addActiveUser(String username) {
        if (!activeUsers.contains(username)) {
            activeUsers.add(username);
        }
    }
    
    void removeActiveUser(String username) {
        activeUsers.remove(username);
    }
    
    List<String> getActiveUsers() {
        return new ArrayList<>(activeUsers);
    }
    
    void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client removed. Total clients: " + clients.size());
        
        // Clean up disconnected clients
        clients.removeIf(c -> !c.isConnected());
    }
    
    public void stop() {
        System.out.println("Stopping chat server...");
        isRunning = false;
        
        // Notify all clients that server is shutting down
        for (ClientHandler client : clients) {
            if (client.isConnected()) {
                client.sendMessage("SYSTEM:Server is shutting down...");
            }
        }
        
        cleanup();
    }
    
    private void cleanup() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            
            // Disconnect all clients
            for (ClientHandler client : clients) {
                client.disconnect();
            }
            
            clients.clear();
            activeUsers.clear();
            
        } catch (IOException e) {
            System.err.println("Error during server cleanup: " + e.getMessage());
        }
        
        System.out.println("Chat server stopped.");
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public int getClientCount() {
        return clients.size();
    }
    
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        server.start();
    }
}
