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

    // Запуск каждые 15 дней в 10:00
    @Scheduled(cron = "0 0 10 */15 * ?")
    public void sendGroupReminders() {
        if (bot == null) {
            System.err.println("Bot not initialized for scheduler");
            return;
        }

        System.out.println("🔔 Checking groups for reminders...");

        List<Long> groupsToRemind = Database.getGroupsNeedingReminder();

        System.out.println("Found " + groupsToRemind.size() + " groups to remind");

        for (Long chatId : groupsToRemind) {
            try {
                sendGroupReminderMessage(chatId);
                Database.updateGroupReminderDate(chatId);
                System.out.println("✅ Sent reminder to group: " + chatId);
                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("❌ Failed to send reminder to group " + chatId + ": " + e.getMessage());
                if (e.getMessage() != null && (e.getMessage().contains("bot was blocked") ||
                        e.getMessage().contains("bot was kicked") ||
                        e.getMessage().contains("chat not found"))) {
                    Database.deactivateGroup(chatId);
                }
            }
        }

        System.out.println("✅ Group reminder sending completed");
    }

    private void sendGroupReminderMessage(Long chatId) throws TelegramApiException {
        String reminderText = """
                👋 Hi everyone!
                
                As a reminder, you can write me a private message with any questions, suggestions, or ideas!
                
                📝 Report any employee concerns here.
                
                I'll be glad to help! 😊
                """;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(reminderText);

        bot.execute(message);
    }
}