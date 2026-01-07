package com.example.logistbot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String URL = "jdbc:postgresql://localhost:5432/telegram_bot";
    private static final String USER = "postgres";
    private static final String PASSWORD = "root123";

    public static Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initDatabase() {
        String createUsersTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                chat_id BIGINT UNIQUE NOT NULL,
                name VARCHAR(255),
                status VARCHAR(50) DEFAULT 'START',
                is_admin BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createMessagesTableSQL = """
            CREATE TABLE IF NOT EXISTS user_messages (
                id SERIAL PRIMARY KEY,
                chat_id BIGINT NOT NULL,
                status_code VARCHAR(100),
                message TEXT NOT NULL,
                is_read BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createGroupsTableSQL = """
            CREATE TABLE IF NOT EXISTS telegram_groups (
                id SERIAL PRIMARY KEY,
                chat_id BIGINT UNIQUE NOT NULL,
                group_name VARCHAR(255),
                added_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_reminder_date TIMESTAMP,
                is_active BOOLEAN DEFAULT TRUE
            )
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(createUsersTableSQL);
             PreparedStatement stmt2 = conn.prepareStatement(createMessagesTableSQL);
             PreparedStatement stmt3 = conn.prepareStatement(createGroupsTableSQL)) {
            stmt1.execute();
            stmt2.execute();
            stmt3.execute();
            System.out.println("Database initialized!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getUserStatus(Long chatId) {
        String sql = "SELECT status FROM users WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "START";
    }

    public static void createUser(Long chatId) {
        String sql = "INSERT INTO users (chat_id, status) VALUES (?, 'START') ON CONFLICT (chat_id) DO NOTHING";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateUserStatus(Long chatId, String status) {
        String sql = "UPDATE users SET status = ? WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setLong(2, chatId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateUserName(Long chatId, String name) {
        String sql = "UPDATE users SET name = ? WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setLong(2, chatId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============ GROUP METHODS ============

    public static void saveGroup(Long chatId, String groupName) {
        String sql = "INSERT INTO telegram_groups (chat_id, group_name) VALUES (?, ?) ON CONFLICT (chat_id) DO UPDATE SET is_active = TRUE, group_name = EXCLUDED.group_name";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setString(2, groupName);
            stmt.executeUpdate();
            System.out.println("Group saved: " + groupName + " (" + chatId + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Long> getGroupsNeedingReminder() {
        List<Long> groups = new ArrayList<>();
        String sql = """
            SELECT chat_id FROM telegram_groups 
            WHERE is_active = TRUE 
            AND (last_reminder_date IS NULL OR last_reminder_date < NOW() - INTERVAL '15 days')
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                groups.add(rs.getLong("chat_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groups;
    }

    public static void updateGroupReminderDate(Long chatId) {
        String sql = "UPDATE telegram_groups SET last_reminder_date = CURRENT_TIMESTAMP WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deactivateGroup(Long chatId) {
        String sql = "UPDATE telegram_groups SET is_active = FALSE WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.executeUpdate();
            System.out.println("Group deactivated: " + chatId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getActiveGroupsCount() {
        String sql = "SELECT COUNT(*) FROM telegram_groups WHERE is_active = TRUE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static List<GroupInfo> getAllGroups() {
        List<GroupInfo> groups = new ArrayList<>();
        String sql = "SELECT chat_id, group_name, added_date, last_reminder_date, is_active FROM telegram_groups ORDER BY added_date DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                GroupInfo group = new GroupInfo();
                group.setChatId(rs.getLong("chat_id"));
                group.setGroupName(rs.getString("group_name"));
                group.setAddedDate(rs.getTimestamp("added_date"));
                group.setLastReminderDate(rs.getTimestamp("last_reminder_date"));
                group.setActive(rs.getBoolean("is_active"));
                groups.add(group);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groups;
    }

    // ============ ADMIN METHODS ============

    public static List<UserMessage> getAllMessages() {
        List<UserMessage> messages = new ArrayList<>();
        String sql = "SELECT um.*, u.name FROM user_messages um " +
                "LEFT JOIN users u ON um.chat_id = u.chat_id " +
                "ORDER BY um.created_at DESC LIMIT 50";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UserMessage msg = new UserMessage();
                msg.setId(rs.getInt("id"));
                msg.setChatId(rs.getLong("chat_id"));
                msg.setStatusCode(rs.getString("status_code"));
                msg.setMessage(rs.getString("message"));
                msg.setCreatedAt(rs.getTimestamp("created_at"));
                msg.setUserName(rs.getString("name"));
                msg.setRead(rs.getBoolean("is_read"));
                messages.add(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    public static List<UserMessage> getMessagesByCategory(String category) {
        List<UserMessage> messages = new ArrayList<>();
        String sql = "SELECT um.*, u.name FROM user_messages um " +
                "LEFT JOIN users u ON um.chat_id = u.chat_id " +
                "WHERE um.status_code LIKE ? " +
                "ORDER BY um.created_at DESC LIMIT 50";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UserMessage msg = new UserMessage();
                msg.setId(rs.getInt("id"));
                msg.setChatId(rs.getLong("chat_id"));
                msg.setStatusCode(rs.getString("status_code"));
                msg.setMessage(rs.getString("message"));
                msg.setCreatedAt(rs.getTimestamp("created_at"));
                msg.setUserName(rs.getString("name"));
                msg.setRead(rs.getBoolean("is_read"));
                messages.add(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    public static List<UserMessage> getUnreadMessages() {
        List<UserMessage> messages = new ArrayList<>();
        String sql = "SELECT um.*, u.name FROM user_messages um " +
                "LEFT JOIN users u ON um.chat_id = u.chat_id " +
                "WHERE um.is_read = FALSE " +
                "ORDER BY um.created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UserMessage msg = new UserMessage();
                msg.setId(rs.getInt("id"));
                msg.setChatId(rs.getLong("chat_id"));
                msg.setStatusCode(rs.getString("status_code"));
                msg.setMessage(rs.getString("message"));
                msg.setCreatedAt(rs.getTimestamp("created_at"));
                msg.setUserName(rs.getString("name"));
                msg.setRead(rs.getBoolean("is_read"));
                messages.add(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    public static void markMessageAsRead(int messageId) {
        String sql = "UPDATE user_messages SET is_read = TRUE WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void markAllMessagesAsRead() {
        String sql = "UPDATE user_messages SET is_read = TRUE WHERE is_read = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getUnreadCount() {
        String sql = "SELECT COUNT(*) FROM user_messages WHERE is_read = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getTotalMessagesCount() {
        String sql = "SELECT COUNT(*) FROM user_messages";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getTotalUsersCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE status = 'REGISTERED'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean isUserAdmin(Long chatId) {
        String sql = "SELECT is_admin FROM users WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_admin");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void setUserAdmin(Long chatId, boolean isAdmin) {
        String sql = "UPDATE users SET is_admin = ? WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isAdmin);
            stmt.setLong(2, chatId);
            stmt.executeUpdate();
            System.out.println("User " + chatId + " admin status set to: " + isAdmin);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Long> getAllAdmins() {
        List<Long> admins = new ArrayList<>();
        String sql = "SELECT chat_id FROM users WHERE is_admin = TRUE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                admins.add(rs.getLong("chat_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return admins;
    }
}