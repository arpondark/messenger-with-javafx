package com.arpon7fx.ar.messenger;

/**
 * Simple launcher for testing the messenger application
 * You can run multiple instances of this to test the chat functionality
 */
public class MessengerLauncher {
    
    public static void main(String[] args) {
        System.out.println("Starting Arpon's Messenger...");
        System.out.println("To test locally:");
        System.out.println("1. Run this program twice (two instances)");
        System.out.println("2. In first instance: Enter username → Click 'Host'");
        System.out.println("3. In second instance: Enter username → Click 'Connect'");
        System.out.println("4. Start chatting!");
        System.out.println();
        
        // Launch the JavaFX application
        Main.main(args);
    }
}
