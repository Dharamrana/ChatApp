package com.chatapp.client;
import com.chatapp.model.Message;
import com.chatapp.util.FileUtils;
import java.io.*;
import java.net.*;
public class ChatClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private String   username;
    private ChatGUI  gui;

    public ChatClient(String username, ChatGUI gui, Socket socket, ObjectOutputStream out, ObjectInputStream in) {
        this.username = username;
        this.gui      = gui;
        this.socket   = socket;
        this.out      = out;
        this.in       = in;
    }

    public void startListening() {
        new Thread(this::listen).start();
    }

    private void listen() {
        try {
            while (true) { gui.receiveMessage((Message) in.readObject()); }
        } catch (EOFException | SocketException e) { gui.showError("Disconnected from server.");
        } catch (Exception e) { gui.showError("Connection error: " + e.getMessage()); }
    }

    public void sendText(String text) { sendMessage(new Message(Message.Type.TEXT, username, text)); }

    public void sendPrivate(String target, String text) {
        Message msg = new Message(Message.Type.TEXT, username, text);
        msg.setReceiver(target); sendMessage(msg);
    }

    public void sendFile(String filePath, String target) {
        try {
            byte[] data = FileUtils.readFile(filePath);
            String name = FileUtils.getFileName(filePath);
            Message msg = new Message(Message.Type.FILE, username, "File: " + name);
            msg.setFileName(name); msg.setFileData(data);
            if (target != null && !target.isEmpty()) msg.setReceiver(target);
            sendMessage(msg);
        } catch (IOException e) { gui.showError("File error: " + e.getMessage()); }
    }

    public void sendMessage(Message msg) {
        try { out.writeObject(msg); out.flush(); }
        catch (IOException e) { gui.showError("Send failed."); }
    }

    public void disconnect() {
        try { sendText("/quit"); if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}