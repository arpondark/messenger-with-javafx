# Arpon's Messenger 💬

A real-time local messaging application built with JavaFX that enables instant communication between multiple users on the same network.

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.6-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 🌟 Features

### 💬 **Real-Time Messaging**
- Instant message delivery between connected users
- Beautiful message bubbles with sender identification
- Timestamps for all messages
- Auto-scroll to latest messages

### 🌐 **Network Communication**
- **Host Mode**: Start your own chat server
- **Client Mode**: Connect to existing servers
- Support for multiple simultaneous users (up to 50)
- Local network and internet connectivity

### 🎯 **Advanced Chat Features**
- **Private Messaging**: Send whispers to specific users (`/whisper <username> <message>`)
- **User Management**: See who's online (`/users`)
- **Server Commands**: Ping server, get time, and more
- **System Notifications**: Join/leave alerts

### 🎨 **Modern UI**
- Clean, professional interface
- Dark sidebar with contact-friendly design
- Color-coded message bubbles (blue for you, gray for others)
- Connection status indicators
- Responsive layout

## 📋 Requirements

- **Java 17** or higher
- **Maven 3.8+** for building
- **JavaFX 17.0.6** (included in dependencies)

## 🚀 Quick Start

### 1. Clone & Build
```bash
git clone https://github.com/arpondark/messenger-with-javafx
cd messenger
mvn clean compile
```

### 2. Run the Application
```bash
mvn javafx:run
```

### 3. Start Chatting
**Option A: Host a Chat**
1. Enter your username
2. Click **"Host"** button
3. Share your IP address with friends
4. Start chatting when they connect!

**Option B: Join a Chat**
1. Enter your username
2. Enter the host's IP address
3. Click **"Connect"** button
4. Start chatting immediately!

## 🔧 Building JAR File

To create a standalone JAR file:

```bash
mvn clean package
```

The executable JAR will be created in the `target/` directory as `messenger-1.0-SNAPSHOT.jar`.

### Running the JAR
```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar target/messenger-1.0-SNAPSHOT.jar
```

## 💻 Testing Locally

To test the chat functionality on the same computer:

1. **Terminal 1**: Run the first instance
   ```bash
   mvn javafx:run
   ```
   - Enter username (e.g., "Alice")
   - Click **"Host"**

2. **Terminal 2**: Run the second instance
   ```bash
   mvn javafx:run
   ```
   - Enter different username (e.g., "Bob")
   - Click **"Connect"**

3. **Start chatting!** Messages will appear instantly in both windows.

## 📱 Chat Commands

| Command | Description | Example |
|---------|-------------|---------|
| `message` | Send regular message | `Hello everyone!` |
| `/ping` | Test server connection | `/ping` |
| `/users` | List online users | `/users` |
| `/whisper` | Send private message | `/whisper Alice Hey there!` |
| `/time` | Get server time | `/time` |

## 🏗️ Project Structure

```
messenger/
├── src/main/java/com/arpon7fx/ar/messenger/
│   ├── Main.java              # JavaFX Application entry point
│   ├── Controller.java        # UI Controller with real-time messaging
│   ├── ChatServer.java        # Server implementation
│   ├── ChatClient.java        # Client implementation
│   └── MessengerLauncher.java # Alternative launcher
├── src/main/resources/com/arpon7fx/ar/messenger/
│   └── hello-view.fxml        # UI layout
├── pom.xml                    # Maven configuration
└── README.md                  # This file
```

## 🔌 Network Configuration

### For Local Network Use:
1. **Host**: Click "Host" and share your local IP address
2. **Clients**: Enter host's IP address and connect

### Finding Your IP Address:
- **Windows**: `ipconfig` in Command Prompt
- **macOS/Linux**: `ifconfig` or `ip addr show`

### Port Configuration:
- **Default Port**: 12345
- **Firewall**: Ensure port 12345 is open for incoming connections

## 🛠️ Technical Details

### Architecture
- **Client-Server Model**: One host, multiple clients
- **Socket Communication**: TCP sockets for reliable messaging
- **Multi-threading**: Separate threads for UI and network operations
- **Thread-Safe Operations**: Concurrent collections and atomic operations

### Key Classes
- **ChatServer**: Manages client connections and message broadcasting
- **ChatClient**: Handles server communication and message listening
- **Controller**: JavaFX controller managing UI and user interactions
- **Main**: Application entry point with error handling

### Dependencies
```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.6</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>17.0.6</version>
</dependency>
```

## 🐛 Troubleshooting

### Common Issues

**Connection Failed**
- Check if server is running
- Verify IP address and port
- Check firewall settings

**Application Won't Start**
- Ensure Java 17+ is installed
- Verify JavaFX is properly configured
- Check console for error messages

**Messages Not Appearing**
- Confirm both users are connected
- Check network connectivity
- Try disconnecting and reconnecting

### Error Messages
- `"Cannot reach server"`: Host is not running or unreachable
- `"Username already taken"`: Choose a different username
- `"Server is full"`: Server has reached maximum capacity (50 users)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Arpon** - [GitHub Profile](https://github.com/arpon7fx)

## 🙏 Acknowledgments

- JavaFX team for the excellent UI framework
- Maven for build automation
- Socket programming community for networking insights

---

**Happy Chatting!** 🎉

For questions or support, please open an issue on GitHub.