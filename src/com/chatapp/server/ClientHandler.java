package com.chatapp.server;
import com.chatapp.model.Message;
import com.chatapp.util.FileUtils;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream  in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket) { this.socket = socket; }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());
            if (!handleAuth()) { socket.close(); return; }
            ChatServer.onlineUsers.put(username, this);
            System.out.println("[+] " + username + " joined. Online: " + ChatServer.onlineUsers.size());

            // Notify others
            ChatServer.broadcast(new Message(Message.Type.JOIN, "SERVER", username + " joined the workspace!"), username);

            // Send status
            sendMessage(new Message(Message.Type.STATUS, "SERVER", "Online users: " + ChatServer.getOnlineUsers()));
            sendMessage(new Message(Message.Type.STATUS, "SERVER", "Groups: " + ChatServer.getGroupsList()));

            // Send chat history (last 50 messages)
            sendHistory("general");

            while (true) { handleMessage((Message) in.readObject()); }

        } catch (EOFException | SocketException e) {
        } catch (Exception e) { System.err.println("Error [" + username + "]: " + e.getMessage()); }
        finally { disconnect(); }
    }

    // ── Auth ───────────────────────────────────────────────────────────────
    private boolean handleAuth() throws Exception {
        Message authMsg = (Message) in.readObject();
        String[] parts  = authMsg.getContent().split("\\|", 2);
        if (parts.length < 2) {
            sendMessage(new Message(Message.Type.AUTH_FAIL, "SERVER", "Invalid format.")); return false;
        }
        String user = parts[0].trim();
        String pass = parts[1].trim();

        if (authMsg.getType() == Message.Type.AUTH_REGISTER) {
            if (user.length() < 3) { sendMessage(new Message(Message.Type.AUTH_FAIL, "SERVER", "Username must be 3+ characters.")); return false; }
            if (pass.length() < 4) { sendMessage(new Message(Message.Type.AUTH_FAIL, "SERVER", "Password must be 4+ characters.")); return false; }
            if (!ChatServer.userDB.register(user, pass)) {
                sendMessage(new Message(Message.Type.AUTH_FAIL, "SERVER", "Username already taken!")); return false;
            }
            username = user;
            sendMessage(new Message(Message.Type.AUTH_SUCCESS, "SERVER", "Account created! Welcome, " + username + "!"));
            return true;
        } else {
            if (!ChatServer.userDB.login(user, pass)) {
                sendMessage(new Message(Message.Type.AUTH_FAIL, "SERVER", "Wrong username or password.")); return false;
            }
            if (ChatServer.onlineUsers.containsKey(user)) {
                sendMessage(new Message(Message.Type.AUTH_FAIL, "SERVER", "Already logged in!")); return false;
            }
            username = user;
            sendMessage(new Message(Message.Type.AUTH_SUCCESS, "SERVER", "Welcome back, " + username + "!"));
            return true;
        }
    }

    // ── Send chat history to newly joined user ─────────────────────────────
    private void sendHistory(String channel) {
        List<Message> history = ChatServer.chatDB.loadChannelHistory(channel, 50);
        if (history.isEmpty()) return;
        sendMessage(new Message(Message.Type.STATUS, "SERVER", "--- Chat History ---"));
        for (Message m : history) sendMessage(m);
        sendMessage(new Message(Message.Type.STATUS, "SERVER", "--- End of History ---"));
    }

    // ── Handle incoming messages ───────────────────────────────────────────
    private void handleMessage(Message msg) throws IOException {
        switch (msg.getType()) {
            case TEXT:
                if (msg.getContent().startsWith("/")) { handleCommand(msg); break; }
                if (msg.getReceiver() != null) {
                    // Private message — save and send to BOTH sender and receiver
                    msg.setType(Message.Type.PRIVATE);
                    ChatServer.chatDB.saveMessage(msg);
                    ChatServer.sendToUser(msg.getReceiver(), msg);
                    sendMessage(msg); // sender also sees it                
                } else {
                    ChatServer.chatDB.saveMessage(msg);
                    ChatServer.broadcastAll(msg); // sender sees it too
                }
                break;

            case FILE: case VOICE:
                FileUtils.saveFile(msg.getFileName(), msg.getFileData());
                ChatServer.chatDB.saveMessage(msg);
                if (msg.getReceiver() != null) {
                    ChatServer.sendToUser(msg.getReceiver(), msg);
                    sendMessage(msg);
                } else {
                    ChatServer.broadcastAll(msg);
                }
                break;

            case GROUP:
                Set<String> members = ChatServer.groups.get(msg.getReceiver());
                if (members != null && members.contains(username)) {
                    ChatServer.chatDB.saveMessage(msg);
                    ChatServer.sendToGroup(msg.getReceiver(), msg);
                    sendMessage(msg); // sender sees it
                } else {
                    sendMessage(new Message(Message.Type.TEXT, "SERVER", "You are not in #" + msg.getReceiver()));
                }
                break;

            case REACTION:
                ChatServer.broadcast(msg, null);
                break;

            case TYPING:
                ChatServer.broadcast(msg, username);
                break;

            case READ:
                if (msg.getReceiver() != null) ChatServer.sendToUser(msg.getReceiver(), msg);
                break;

            default: break;
        }
    }

    // ── Handle commands ────────────────────────────────────────────────────
    private void handleCommand(Message msg) {
        String[] parts = msg.getContent().trim().split("\\s+", 3);
        switch (parts[0].toLowerCase()) {
            case "/help":
                sendMessage(new Message(Message.Type.TEXT, "SERVER",
                    "Commands:\n  /users\n  /pm <user> <msg>\n  /channels\n  /join <channel>\n  /leave <channel>\n  /gc <channel> <msg>\n  /history <channel>\n  /quit"));
                break;
            case "/users":
                sendMessage(new Message(Message.Type.STATUS, "SERVER", "Online: " + ChatServer.getOnlineUsers())); break;
            case "/channels":
                sendMessage(new Message(Message.Type.STATUS, "SERVER", "Channels: " + ChatServer.getGroupsList())); break;
            case "/join":
                if (parts.length >= 2) {
                    ChatServer.groups.computeIfAbsent(parts[1], k -> new HashSet<>()).add(username);
                    sendMessage(new Message(Message.Type.TEXT, "SERVER", "Joined #" + parts[1]));
                    sendHistory(parts[1]);
                } break;
            case "/leave":
                if (parts.length >= 2) {
                    Set<String> m = ChatServer.groups.get(parts[1]);
                    if (m != null) m.remove(username);
                    sendMessage(new Message(Message.Type.TEXT, "SERVER", "Left #" + parts[1]));
                } break;
            case "/pm":
                if (parts.length >= 3) {
                    Message pm = new Message(Message.Type.PRIVATE, username, parts[2]);
                    pm.setReceiver(parts[1]);
                    ChatServer.chatDB.saveMessage(pm);
                    ChatServer.sendToUser(parts[1], pm);
                    sendMessage(pm); // sender sees it
                } break;
            case "/gc":
                if (parts.length >= 3) {
                    Message gm = new Message(Message.Type.GROUP, username, parts[2]);
                    gm.setReceiver(parts[1]);
                    ChatServer.chatDB.saveMessage(gm);
                    ChatServer.sendToGroup(parts[1], gm);
                    sendMessage(gm);
                } break;
            case "/history":
                if (parts.length >= 2) sendHistory(parts[1]);
                else sendHistory("general");
                break;
            case "/quit": disconnect(); break;
            default: sendMessage(new Message(Message.Type.TEXT, "SERVER", "Unknown command. Type /help"));
        }
    }

    public void sendMessage(Message msg) {
        try { out.writeObject(msg); out.flush(); }
        catch (IOException e) { System.err.println("Send failed to " + username); }
    }

    private void disconnect() {
        if (username != null) {
            ChatServer.onlineUsers.remove(username);
            ChatServer.groups.values().forEach(m -> m.remove(username));
            System.out.println("[-] " + username + " left.");
            ChatServer.broadcast(new Message(Message.Type.LEAVE, "SERVER", username + " left the workspace."), username);
        }
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}