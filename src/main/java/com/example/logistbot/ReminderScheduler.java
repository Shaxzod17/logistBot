package com.example.logistbot;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class ReminderScheduler {

    private LogistBot bot;

    public void setBot(LogistBot bot) {
        this.bot = bot;
    }

    // Run on 1st and 15th of every month at 10:00 AM
    @Scheduled(cron = "0 0 10 1,15 * ?")
    public void sendReminders() {
        if (bot == null) {
            System.err.println("Bot not initialized for scheduler");
            return;
        }

        System.out.println("🔔 Checking for users who need reminders...");

        List<Long> usersToRemind = Database.getUsersNeedingReminder();

        System.out.println("Found " + usersToRemind.size() + " users to remind");

        for (Long chatId : usersToRemind) {
            try {
                sendReminderMessage(chatId);
                Database.updateLastReminderDate(chatId);
                System.out.println("✅ Sent reminder to user: " + chatId);
                Thread.sleep(100);
            } catch (Exception e) {
                System.err.println("❌ Failed to send reminder to " + chatId + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Reminder sending completed");
    }

    private void sendReminderMessage(Long chatId) throws TelegramApiException {
        String reminderText = """
                👋 Hello!
                
                It's been a while since we heard from you. 
                
                📝 If you have any questions, feedback, or new ideas, feel free to share them with us!
                
                We're here to help! 😊
                """;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(reminderText);

        bot.execute(message);
    }
}