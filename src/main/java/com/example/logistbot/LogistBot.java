package com.example.logistbot;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class LogistBot extends TelegramLongPollingBot {

    private static LogistBot instance;

    public LogistBot() {
        instance = this;
    }

    public static LogistBot getInstance() {
        return instance;
    }

    // Method to check if user is admin (from database)
    private boolean isAdmin(Long chatId) {
        return Database.isUserAdmin(chatId);
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;

        Long chatId = update.getMessage().getChatId();

        // ============ HANDLE ADMIN COMMANDS FIRST ============
        if (isAdmin(chatId) && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            if (handleAdminCommand(chatId, text)) {
                return; // Admin command was handled, stop processing
            }
        }

        // ============ REGULAR USER FLOW ============
        // Create user if not exists
        Database.createUser(chatId);
        String status = Database.getUserStatus(chatId);

        // Handle /start command
        if (update.getMessage().hasText() && update.getMessage().getText().equals("/start")) {
            Database.updateUserStatus(chatId, "WAITING_NAME");
            sendText(chatId, "Hello! 👋\n\nPlease, enter your name:");
            return;
        }

        // Handle /new-admin command (only for registered users)
        if (update.getMessage().hasText() && update.getMessage().getText().equals("/new_admin")) {
            if (status.equals("REGISTERED") || isAdmin(chatId)) {
                Database.setUserAdmin(chatId, true);
                sendText(chatId, "✅ Congratulations! You now have admin rights!\n\n" +
                        "To see admin commands, type /admin.");
                System.out.println("New admin registered: " + chatId);
                return;
            } else {
                sendText(chatId, "⚠️ Please register first! Type /start.");
                return;
            }
        }

        // Handle name input
        if (status.equals("WAITING_NAME") && update.getMessage().hasText()) {
            String name = update.getMessage().getText();
            Database.updateUserName(chatId, name);
            Database.updateUserStatus(chatId, "REGISTERED");
            sendMainMenu(chatId);
            return;
        }

        // Handle menu buttons
        if (status.equals("REGISTERED") && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            handleMenuSelection(chatId, text);
            return;
        }

        // Handle submenu buttons
        if ((status.equals("DISPATCH_MENU") || status.equals("ACCOUNTING_MENU") ||
                status.equals("FLEET_MENU") || status.equals("SAFETY_MENU") ||
                status.equals("ELD_MENU") || status.equals("HR_MENU")) && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            handleSubmenuSelection(chatId, text, status);
            return;
        }

        // Handle message input after selecting option
        if (status.startsWith("DISPATCH_") || status.startsWith("ACCOUNTING_") ||
                status.startsWith("FLEET_") || status.startsWith("SAFETY_") ||
                status.startsWith("ELD_") || status.startsWith("HR_") ||
                status.startsWith("WAITING_")) {
            if (update.getMessage().hasText()) {
                String message = update.getMessage().getText();
                saveUserMessage(chatId, status, message);
                sendText(chatId, "✅ Your message has been received! Thank you!");
                Database.updateUserStatus(chatId, "REGISTERED");
                sendMainMenu(chatId);
            }
            return;
        }

        // Handle new idea input
        if (status.equals("WAITING_NEW_IDEA") && update.getMessage().hasText()) {
            String idea = update.getMessage().getText();
            saveUserMessage(chatId, "NEW_IDEA", idea);
            sendText(chatId, "✅ Your idea has been accepted! Thank you!");
            Database.updateUserStatus(chatId, "REGISTERED");
            sendMainMenu(chatId);
            return;
        }
    }

    // ============ ADMIN COMMAND HANDLER ============
    private boolean handleAdminCommand(Long chatId, String command) {
        switch (command) {
            case "/admin" -> {
                sendAdminMenu(chatId);
                return true;
            }
            case "/all" -> {
                sendAllMessages(chatId);
                return true;
            }
            case "/unread" -> {
                sendUnreadMessages(chatId);
                return true;
            }
            case "/dispatch" -> {
                sendMessagesByCategory(chatId, "DISPATCH");
                return true;
            }
            case "/accounting" -> {
                sendMessagesByCategory(chatId, "ACCOUNTING");
                return true;
            }
            case "/fleet" -> {
                sendMessagesByCategory(chatId, "FLEET");
                return true;
            }
            case "/safety" -> {
                sendMessagesByCategory(chatId, "SAFETY");
                return true;
            }
            case "/eld" -> {
                sendMessagesByCategory(chatId, "ELD");
                return true;
            }
            case "/hr" -> {
                sendMessagesByCategory(chatId, "HR");
                return true;
            }
            case "/ideas" -> {
                sendMessagesByCategory(chatId, "NEW_IDEA");
                return true;
            }
            case "/stats" -> {
                sendStatistics(chatId);
                return true;
            }
            case "/markread" -> {
                Database.markAllMessagesAsRead();
                sendText(chatId, "✅ All messages marked as read!");
                return true;
            }
            case "/listadmins" -> {
                listAllAdmins(chatId);
                return true;
            }
            case "/removeadmin" -> {
                Database.setUserAdmin(chatId, false);
                sendText(chatId, "✅ Your admin rights have been removed!");
                return true;
            }
            default -> {
                return false; // Not an admin command
            }
        }
    }

    // ============ ADMIN METHODS ============
    @SneakyThrows
    private void sendAdminMenu(Long chatId) {
        String menu = """
                🎛 **ADMIN PANEL** 🎛
                
                📋 **View Messages:**
                /all - View all messages (last 50)
                /unread - View unread messages only
                /markread - Mark all as read
                
                📂 **By Department:**
                /dispatch - Dispatch Team messages
                /accounting - Accounting messages
                /fleet - Fleet Team messages
                /safety - Safety messages
                /eld - ELD Team messages
                /hr - HR messages
                /ideas - New Ideas
                
                📊 **Statistics:**
                /stats - View statistics
                
                👥 **Admin Management:**
                /listadmins - List all admins
                /removeadmin - Remove your admin access
                
                ━━━━━━━━━━━━━━━━━━
                Use /admin anytime to see this menu.
                """;
        sendText(chatId, menu);
    }

    @SneakyThrows
    private void sendAllMessages(Long chatId) {
        List<UserMessage> messages = Database.getAllMessages();
        if (messages.isEmpty()) {
            sendText(chatId, "📭 No messages found.");
            return;
        }

        sendText(chatId, "📬 **All Messages** (Last 50):\n\n");

        for (UserMessage msg : messages) {
            String formattedMessage = formatMessageForAdmin(msg);
            sendText(chatId, formattedMessage);
            Thread.sleep(100);
        }

        sendText(chatId, "\n✅ Displayed " + messages.size() + " messages.");
    }

    @SneakyThrows
    private void sendUnreadMessages(Long chatId) {
        List<UserMessage> messages = Database.getUnreadMessages();
        if (messages.isEmpty()) {
            sendText(chatId, "✅ No unread messages!");
            return;
        }

        sendText(chatId, "🔔 **Unread Messages** (" + messages.size() + "):\n\n");

        for (UserMessage msg : messages) {
            String formattedMessage = formatMessageForAdmin(msg);
            sendText(chatId, formattedMessage);
            Database.markMessageAsRead(msg.getId());
            Thread.sleep(100);
        }

        sendText(chatId, "\n✅ All " + messages.size() + " messages marked as read!");
    }

    @SneakyThrows
    private void sendMessagesByCategory(Long chatId, String category) {
        List<UserMessage> messages = Database.getMessagesByCategory(category);
        if (messages.isEmpty()) {
            sendText(chatId, "📭 No messages found for category: " + category);
            return;
        }

        sendText(chatId, "📂 **" + category + " Messages** (" + messages.size() + "):\n\n");

        for (UserMessage msg : messages) {
            String formattedMessage = formatMessageForAdmin(msg);
            sendText(chatId, formattedMessage);
            Thread.sleep(100);
        }

        sendText(chatId, "\n✅ Displayed " + messages.size() + " messages.");
    }

    @SneakyThrows
    private void sendStatistics(Long chatId) {
        int totalMessages = Database.getTotalMessagesCount();
        int totalUnread = Database.getUnreadCount();
        int totalUsers = Database.getTotalUsersCount();
        int totalAdmins = Database.getAllAdmins().size();

        String stats = String.format("""
                        📊 **STATISTICS**
                        
                        👥 Total Registered Users: %d
                        👑 Total Admins: %d
                        📨 Total Messages: %d
                        🔔 Unread Messages: %d
                        ✅ Read Messages: %d
                        
                        📂 **Messages by Department:**
                        📦 Dispatch: %d messages
                        💰 Accounting: %d messages
                        🚛 Fleet: %d messages
                        🛡 Safety: %d messages
                        📱 ELD: %d messages
                        👥 HR: %d messages
                        💡 New Ideas: %d messages
                        
                        ━━━━━━━━━━━━━━━━━━
                        Last updated: %s
                        """,
                totalUsers,
                totalAdmins,
                totalMessages,
                totalUnread,
                totalMessages - totalUnread,
                Database.getMessagesByCategory("DISPATCH").size(),
                Database.getMessagesByCategory("ACCOUNTING").size(),
                Database.getMessagesByCategory("FLEET").size(),
                Database.getMessagesByCategory("SAFETY").size(),
                Database.getMessagesByCategory("ELD").size(),
                Database.getMessagesByCategory("HR").size(),
                Database.getMessagesByCategory("NEW_IDEA").size(),
                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new java.util.Date())
        );

        sendText(chatId, stats);
    }

    @SneakyThrows
    private void listAllAdmins(Long chatId) {
        List<Long> admins = Database.getAllAdmins();
        if (admins.isEmpty()) {
            sendText(chatId, "📭 No admins found.");
            return;
        }

        StringBuilder message = new StringBuilder("👥 **All admins** (" + admins.size() + "):\n\n");
        int count = 1;
        for (Long adminChatId : admins) {
            message.append(count++).append(". Chat ID: ").append(adminChatId);
            if (adminChatId.equals(chatId)) {
                message.append(" (You)");
            }
            message.append("\n");
        }

        sendText(chatId, message.toString());
    }

    private String formatMessageForAdmin(UserMessage msg) {
        String readStatus = msg.isRead() ? "✅" : "🔔 NEW";
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        return String.format("""
                        ━━━━━━━━━━━━━━━━━━
                        %s Message ID: %d
                        👤 User: %s
                        📂 Category: %s
                        💬 Message: "%s"
                        ⏰ Time: %s
                        ━━━━━━━━━━━━━━━━━━
                        """,
                readStatus,
                msg.getId(),
                msg.getUserName() != null ? msg.getUserName() : "Unknown",
                msg.getStatusCode(),
                msg.getMessage(),
                dateFormat.format(msg.getCreatedAt())
        );
    }

    @SneakyThrows
    private void notifyAdmin(Long userChatId, String statusCode, String message) {
        String notification = String.format("""
                        🔔 **NEW MESSAGE RECEIVED!**
                        
                        👤 User Chat ID: %d
                        📂 Category: %s
                        💬 Message: "%s"
                        
                        ━━━━━━━━━━━━━━━━━━
                        Use /unread to view all unread messages or /admin to see the full menu.
                        """,
                userChatId,
                statusCode,
                message.length() > 100 ? message.substring(0, 100) + "..." : message
        );

        // Send notification to all admins
        List<Long> admins = Database.getAllAdmins();
        for (Long adminChatId : admins) {
            try {
                sendText(adminChatId, notification);
            } catch (Exception e) {
                System.err.println("Failed to notify admin " + adminChatId + ": " + e.getMessage());
            }
        }
    }

    // ============ USER METHODS ============
    private void handleMenuSelection(Long chatId, String selection) {
        switch (selection) {
            case "📦 Dispatch Team" -> {
                Database.updateUserStatus(chatId, "DISPATCH_MENU");
                sendDispatchMenu(chatId);
            }
            case "💰 Accounting" -> {
                Database.updateUserStatus(chatId, "ACCOUNTING_MENU");
                sendAccountingMenu(chatId);
            }
            case "🚛 Fleet Team" -> {
                Database.updateUserStatus(chatId, "FLEET_MENU");
                sendFleetMenu(chatId);
            }
            case "🛡 Safety" -> {
                Database.updateUserStatus(chatId, "SAFETY_MENU");
                sendSafetyMenu(chatId);
            }
            case "📱 ELD Team" -> {
                Database.updateUserStatus(chatId, "ELD_MENU");
                sendELDMenu(chatId);
            }
            case "👥 HR" -> {
                Database.updateUserStatus(chatId, "HR_MENU");
                sendHRMenu(chatId);
            }
            case "💡 New Idea" -> {
                Database.updateUserStatus(chatId, "WAITING_NEW_IDEA");
                sendText(chatId, "💡 Enter your idea:");
            }
            case "🔙 Back" -> {
                Database.updateUserStatus(chatId, "REGISTERED");
                sendMainMenu(chatId);
            }
            default -> sendText(chatId, "Invalid choice. Please select one of the buttons.");
        }
    }

    private void handleSubmenuSelection(Long chatId, String selection, String status) {
        if (selection.equals("🔙 Back")) {
            Database.updateUserStatus(chatId, "REGISTERED");
            sendMainMenu(chatId);
            return;
        }

        if (selection.equals("✍️ Other (your idea)")) {
            String category = switch (status) {
                case "DISPATCH_MENU" -> "DISPATCH";
                case "ACCOUNTING_MENU" -> "ACCOUNTING";
                case "FLEET_MENU" -> "FLEET";
                case "SAFETY_MENU" -> "SAFETY";
                case "ELD_MENU" -> "ELD";
                case "HR_MENU" -> "HR";
                default -> "OTHER";
            };
            Database.updateUserStatus(chatId, "WAITING_" + category + "_OTHER");
            sendText(chatId, "✍️ Write your idea:");
            return;
        }

        String statusCode = getStatusCode(status, selection);
        Database.updateUserStatus(chatId, statusCode);
        sendText(chatId, "✍️ Please, type your message:");
    }

    private String getStatusCode(String menuStatus, String selection) {
        String prefix = switch (menuStatus) {
            case "DISPATCH_MENU" -> "DISPATCH";
            case "ACCOUNTING_MENU" -> "ACCOUNTING";
            case "FLEET_MENU" -> "FLEET";
            case "SAFETY_MENU" -> "SAFETY";
            case "ELD_MENU" -> "ELD";
            case "HR_MENU" -> "HR";
            default -> "UNKNOWN";
        };

        String suffix = switch (selection) {
            case "Gross" -> "GROSS";
            case "Mile" -> "MILE";
            case "Relationship" -> "RELATIONSHIP";
            case "Payment" -> "PAYMENT";
            case "Statement" -> "STATEMENT";
            case "Resolve Issues" -> "RESOLVE_ISSUES";
            case "Responsibility" -> "RESPONSIBILITY";
            default -> "OTHER";
        };

        return prefix + "_" + suffix;
    }

    private void saveUserMessage(Long chatId, String statusCode, String message) {
        String sql = "INSERT INTO user_messages (chat_id, status_code, message) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setString(2, statusCode);
            stmt.setString(3, message);
            stmt.executeUpdate();

            System.out.println("Message saved with status: " + statusCode + " - " + message);

            // Track last message date
            Database.updateLastMessageDate(chatId);

            notifyAdmin(chatId, statusCode, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============ TELEGRAM UI METHODS ============
    @SneakyThrows
    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        execute(message);
    }

    @SneakyThrows
    private void sendMainMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("You are registered! ✅\n\nChoose one of the following sections:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📦 Dispatch Team");
        row1.add("💰 Accounting");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🚛 Fleet Team");
        row2.add("🛡 Safety");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("📱 ELD Team");
        row3.add("👥 HR");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("💡 New Idea");

        keyboardRows.add(row1);
        keyboardRows.add(row2);
        keyboardRows.add(row3);
        keyboardRows.add(row4);

        keyboard.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboard);

        execute(message);
    }

    @SneakyThrows
    private void sendDispatchMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📦 Dispatch Team - Choose one of the following sections:");
        message.setReplyMarkup(createSubmenu("Gross", "Mile", "Relationship"));
        execute(message);
    }

    @SneakyThrows
    private void sendAccountingMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("💰 Accounting - Choose one of the following sections:");
        message.setReplyMarkup(createSubmenu("Payment", "Statement"));
        execute(message);
    }

    @SneakyThrows
    private void sendFleetMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🚛 Fleet Team - Choose one of the following sections:");
        message.setReplyMarkup(createSubmenu("Resolve Issues", "Responsibility"));
        execute(message);
    }

    @SneakyThrows
    private void sendSafetyMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🛡 Safety - Choose one of the following sections:");
        message.setReplyMarkup(createSubmenu("Resolve Issues", "Responsibility"));
        execute(message);
    }

    @SneakyThrows
    private void sendELDMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📱 ELD Team - Choose one of the following sections:");
        message.setReplyMarkup(createSubmenu("Resolve Issues", "Responsibility"));
        execute(message);
    }

    @SneakyThrows
    private void sendHRMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👥 HR - Choose one of the following sections:");
        message.setReplyMarkup(createSubmenu("Responsibility", "Relationship"));
        execute(message);
    }

    private ReplyKeyboardMarkup createSubmenu(String... options) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        for (int i = 0; i < options.length; i += 2) {
            KeyboardRow row = new KeyboardRow();
            row.add(options[i]);
            if (i + 1 < options.length) {
                row.add(options[i + 1]);
            }
            keyboardRows.add(row);
        }

        KeyboardRow lastRow = new KeyboardRow();
        lastRow.add("✍️ Other (your idea)");
        keyboardRows.add(lastRow);

        KeyboardRow backRow = new KeyboardRow();
        backRow.add("🔙 Back");
        keyboardRows.add(backRow);

        keyboard.setKeyboard(keyboardRows);
        return keyboard;
    }

    // ============ BOT CREDENTIALS ============
    @Override
    public String getBotUsername() {
        return "smmuzholding_bot";
    }

    @Override
    public String getBotToken() {
        return "7868772017:AAFaXax7CGj4XOSUBenLNOncoyA21BxUXaA";
    }
}