package com.chatapp.model;
import java.io.Serializable;
import java.security.MessageDigest;
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String passwordHash;
    private String avatarColor;
    private long createdAt;
    public User(String username, String password) {
        this.username   = username;
        this.passwordHash = hashPassword(password);
        this.createdAt  = System.currentTimeMillis();
        String[] colors = {"#E57373","#64B5F6","#81C784","#FFB74D","#BA68C8","#4DB6AC"};
        this.avatarColor = colors[Math.abs(username.hashCode()) % colors.length];
    }
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return password; }
    }
    public boolean checkPassword(String password) {
        return this.passwordHash.equals(hashPassword(password));
    }
    public String getUsername()   { return username; }
    public String getAvatarColor() { return avatarColor; }
    public long getCreatedAt()    { return createdAt; }
}