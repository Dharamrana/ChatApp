package com.chatapp.server;
import com.chatapp.model.User;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
public class UserDatabase {
    private static final String DB_FILE = "users.dat";
    private ConcurrentHashMap<String, User> users;

    @SuppressWarnings("unchecked")
    public UserDatabase() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DB_FILE))) {
            users = (ConcurrentHashMap<String, User>) in.readObject();
            System.out.println("[DB] Loaded " + users.size() + " users.");
        } catch (Exception e) {
            users = new ConcurrentHashMap<>();
            System.out.println("[DB] Fresh database created.");
        }
    }

    public synchronized boolean register(String username, String password) {
        if (users.containsKey(username.toLowerCase())) return false;
        users.put(username.toLowerCase(), new User(username, password));
        save(); return true;
    }

    public synchronized boolean login(String username, String password) {
        User user = users.get(username.toLowerCase());
        return user != null && user.checkPassword(password);
    }

    public boolean userExists(String username) {
        return users.containsKey(username.toLowerCase());
    }

    public User getUser(String username) {
        return users.get(username.toLowerCase());
    }

    private void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DB_FILE))) {
            out.writeObject(users);
        } catch (IOException e) { System.err.println("[DB] Save failed: " + e.getMessage()); }
    }
}