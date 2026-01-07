package com.example.logistbot;

import org.apache.catalina.core.ApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@EnableScheduling
public class LogistBotApplication {

    public static void main(String[] args) {
        try {
            System.out.println("Initializing database...");
            Database.initDatabase();

            System.out.println("Starting Spring context...");
            ConfigurableApplicationContext context =
                    SpringApplication.run(LogistBotApplication.class, args);

            System.out.println("Starting Telegram bot...");
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            LogistBot bot = context.getBean(LogistBot.class);
            botsApi.registerBot(bot);

            ReminderScheduler scheduler = context.getBean(ReminderScheduler.class);
            scheduler.setBot(bot);

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("✅ Bot started successfully!");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("📱 Bot is running!");
            System.out.println("🎛️  Admin commands: /admin");
            System.out.println("⏰ Reminder scheduler: Active");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            System.err.println("❌ Failed to start bot:");
            e.printStackTrace();
        }
    }
}
