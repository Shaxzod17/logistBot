package com.example.logistbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class LogistBotApplication {

    public static void main(String[] args) {
        try {
            System.out.println("Initializing database...");
            Database.initDatabase();

            System.out.println("Starting Telegram bot...");
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new LogistBot());

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("✅ Bot started successfully!");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("📱 Bot is running!");
            System.out.println("🎛️  Admin commands: /admin");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            System.err.println("❌ Failed to start bot:");
            e.printStackTrace();
        }
    }
}
