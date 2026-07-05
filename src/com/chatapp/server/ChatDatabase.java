package com.chatapp.server;

import com.chatapp.model.Message;
import java.io.*;
import java.sql.*;
import java.util.*;

public class ChatDatabase {

    private static final String DB_URL = "jdbc:sqlite:chatapp.db";
    private Connection conn;

    public ChatDatabase() {
       try {
            conn = DriverManager.getConnection("jdbc:sqlite:chatapp.db");
            createTables();
            System.out.println("[DB] Chat database connected.");
        } catch (Exception e) {
            System.err.println("[DB] Error: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS messages (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                sender    TEXT NOT NULL,
                receiver  TEXT,
                content   TEXT NOT NULL,
                type      TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                channel   TEXT DEFAULT 'general'
            );
        """;
        conn.createStatement().execute(sql);
        System.out.println("[DB] Tables ready.");
    }

    public void saveMessage(Message msg) {
        String sql = "INSERT INTO messages (sender, receiver, content, type, timestamp, channel) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, msg.getSender());
            ps.setString(2, msg.getReceiver() != null ? msg.getReceiver() : "general");
            ps.setString(3, msg.getContent());
            ps.setString(4, msg.getType().name());
            ps.setString(5, msg.getTimestamp());
            ps.setString(6, msg.getReceiver() != null ? msg.getReceiver() : "general");
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Save error: " + e.getMessage());
        }
    }

    // Load last N messages for a channel
    public List<Message> loadChannelHistory(String channel, int limit) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT * FROM (SELECT * FROM messages WHERE channel=? AND receiver IS NULL OR receiver=? ORDER BY id DESC LIMIT ?) ORDER BY id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channel);
            ps.setString(2, channel);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message m = new Message(
                    Message.Type.valueOf(rs.getString("type")),
                    rs.getString("sender"),
                    rs.getString("content")
                );
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Load error: " + e.getMessage());
        }
        return list;
    }

    // Load DM history between two users
    public List<Message> loadDMHistory(String user1, String user2, int limit) {
        List<Message> list = new ArrayList<>();
        String sql = """
            SELECT * FROM (
                SELECT * FROM messages
                WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?)
                ORDER BY id DESC LIMIT ?
            ) ORDER BY id ASC
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user1); ps.setString(2, user2);
            ps.setString(3, user2); ps.setString(4, user1);
            ps.setInt(5, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message m = new Message(
                    Message.Type.valueOf(rs.getString("type")),
                    rs.getString("sender"),
                    rs.getString("content")
                );
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[DB] DM load error: " + e.getMessage());
        }
        return list;
    }
}