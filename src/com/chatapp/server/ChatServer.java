package com.chatapp.server;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
public class ChatServer {
    public static final int PORT = 12345;
    static Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();
    static Map<String, Set<String>>   groups      = new ConcurrentHashMap<>();
    static UserDatabase               userDB      = new UserDatabase();
    static ChatDatabase               chatDB      = new ChatDatabase();

    public static void main(String[] args) throws IOException {
        System.out.println("================================");
        System.out.println("   ChatApp Server v3.0         ");
        System.out.println("================================");
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("[+] Server on port " + PORT);
        groups.put("general",       new HashSet<>());
        groups.put("random",        new HashSet<>());
        groups.put("announcements", new HashSet<>());
        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("[+] Connection: " + client.getInetAddress().getHostAddress());
            new Thread(new ClientHandler(client)).start();
        }
    }

    // Broadcast to ALL online users
    public static void broadcast(com.chatapp.model.Message msg, String excludeUser) {
        for (Map.Entry<String, ClientHandler> e : onlineUsers.entrySet())
            if (!e.getKey().equals(excludeUser))
                e.getValue().sendMessage(msg);
    }

    // Broadcast to ALL including sender (for sent message confirmation)
    public static void broadcastAll(com.chatapp.model.Message msg) {
        for (ClientHandler h : onlineUsers.values())
            h.sendMessage(msg);
    }

    public static void sendToUser(String username, com.chatapp.model.Message msg) {
        ClientHandler h = onlineUsers.get(username);
        if (h != null) h.sendMessage(msg);
    }

    public static void sendToGroup(String groupName, com.chatapp.model.Message msg) {
        Set<String> members = groups.get(groupName);
        if (members == null) return;
        for (String member : members) sendToUser(member, msg);
    }

    public static String getOnlineUsers() { return String.join(",", onlineUsers.keySet()); }
    public static String getGroupsList()  { return String.join(",", groups.keySet()); }
}