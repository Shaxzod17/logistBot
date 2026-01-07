package com.example.logistbot;

import java.sql.Timestamp;

public class GroupInfo {
    private Long chatId;
    private String groupName;
    private Timestamp addedDate;
    private Timestamp lastReminderDate;
    private boolean isActive;

    public GroupInfo() {
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Timestamp getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(Timestamp addedDate) {
        this.addedDate = addedDate;
    }

    public Timestamp getLastReminderDate() {
        return lastReminderDate;
    }

    public void setLastReminderDate(Timestamp lastReminderDate) {
        this.lastReminderDate = lastReminderDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "GroupInfo{" +
                "chatId=" + chatId +
                ", groupName='" + groupName + '\'' +
                ", addedDate=" + addedDate +
                ", lastReminderDate=" + lastReminderDate +
                ", isActive=" + isActive +
                '}';
    }
}