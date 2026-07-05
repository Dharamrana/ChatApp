package com.chatapp.model;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class Message implements Serializable {
    private static final long serialVersionUID = 4L;
    public enum Type {
        TEXT, FILE, JOIN, LEAVE, STATUS, PRIVATE, GROUP,
        REACTION, TYPING, VOICE, READ,
        AUTH_LOGIN, AUTH_REGISTER, AUTH_SUCCESS, AUTH_FAIL
    }
    private Type   type;
    private String sender;
    private String receiver;
    private String content;
    private String fileName;
    private byte[] fileData;
    private String timestamp;
    private String messageId;
    public Message(Type type, String sender, String content) {
        this.type      = type;
        this.sender    = sender;
        this.content   = content;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        this.messageId = String.valueOf(System.currentTimeMillis());
    }
    public Type   getType()               { return type; }
    public void   setType(Type t)         { this.type = t; }
    public String getSender()             { return sender; }
    public void   setSender(String s)     { this.sender = s; }
    public String getReceiver()           { return receiver; }
    public void   setReceiver(String r)   { this.receiver = r; }
    public String getContent()            { return content; }
    public void   setContent(String c)    { this.content = c; }
    public String getFileName()           { return fileName; }
    public void   setFileName(String fn)  { this.fileName = fn; }
    public byte[] getFileData()           { return fileData; }
    public void   setFileData(byte[] fd)  { this.fileData = fd; }
    public String getTimestamp()          { return timestamp; }
    public String getMessageId()          { return messageId; }
    public void   setMessageId(String id) { this.messageId = id; }
    public String toString() { return "[" + timestamp + "] " + sender + ": " + content; }
}