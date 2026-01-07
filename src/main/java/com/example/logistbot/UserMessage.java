package com.example.logistbot;

import java.sql.Timestamp;

public class UserMessage {
    private int id;
    private Long chatId;
    private String statusCode;
    private String message;
    private Timestamp createdAt;
    private String userName;
    private boolean isRead;

    public UserMessage() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    @Override
    public String toString() {
        return "UserMessage{" +
                "id=" + id +
                ", chatId=" + chatId +
                ", statusCode='" + statusCode + '\'' +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                ", userName='" + userName + '\'' +
                ", isRead=" + isRead +
                '}';
    }
}