package com.hotel.db;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/** Connection factory, schema setup, menu seeding and small transaction helpers. */
public final class Database {

    /** A unit of database work that receives an open connection. */
    public interface SqlWork<T> {
        T apply(Connection c) throws SQLException;
    }

    /** The menu. Each id is unique and every name appears only once. */
    private static final String[][] MENU = {
            {"1", "Veg Sandwich", "60", "FOOD"},
            {"2", "Cheese Sandwich", "80", "FOOD"},
            {"3", "Club Sandwich", "150", "FOOD"},
            {"4", "Veg Burger", "80", "FOOD"},
            {"5", "Chicken Burger", "120", "FOOD"},
            {"6", "Margherita Pizza", "220", "FOOD"},
            {"7", "Veg Pasta", "110", "FOOD"},
            {"8", "Chicken Pasta", "150", "FOOD"},
            {"9", "Veg Hakka Noodles", "110", "FOOD"},
            {"10", "Chicken Hakka Noodles", "140", "FOOD"},
            {"11", "Veg Fried Rice", "110", "FOOD"},
            {"12", "Chicken Fried Rice", "140", "FOOD"},
            {"13", "Paneer Tikka", "180", "FOOD"},
            {"14", "Dal Tadka with Rice", "130", "FOOD"},
            {"15", "Butter Naan", "35", "FOOD"},
            {"16", "Masala Dosa", "90", "FOOD"},
            {"17", "French Fries", "80", "FOOD"},
            {"18", "Cheese Omelette", "70", "FOOD"},
            {"19", "Masala Tea", "20", "DRINK"},
            {"20", "Filter Coffee", "30", "DRINK"},
            {"21", "Cold Coffee", "90", "DRINK"},
            {"22", "Sweet Lassi", "60", "DRINK"},
            {"23", "Masala Chaas", "40", "DRINK"},
            {"24", "Fresh Lime Soda", "50", "DRINK"},
            {"25", "Mineral Water", "25", "DRINK"},
            {"26", "Orange Juice", "60", "DRINK"},
            {"27", "Chocolate Milkshake", "110", "DRINK"},
            {"28", "Hot Chocolate", "95", "DRINK"},
            {"29", "Coca-Cola", "30", "DRINK"},
    };

    private final String host;
    private final int port;
    private final String name;
    private final String user;
    private final String password;
    private final String params;

    private Database(Properties p) {
        this.host = p.getProperty("db.host", "localhost").trim();
        this.port = Integer.parseInt(p.getProperty("db.port", "3306").trim());
        this.name = p.getProperty("db.name", "hotel_db").trim();
        this.user = p.getProperty("db.user", "root").trim();
        this.password = p.getProperty("db.password", "");
        this.params = p.getProperty("db.params", "useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
    }

    /** Reads db.properties from the working directory, or the copy bundled in the jar. */
    public static Database load() throws IOException {
        Properties p = new Properties();
        var external = Paths.get("db.properties");
        InputStream in = Files.isRegularFile(external)
                ? Files.newInputStream(external)
                : Database.class.getResourceAsStream("/db.properties");
        if (in == null) {
            throw new IOException("db.properties not found");
        }
        try (in) {
            p.load(in);
        }
        return new Database(p);
    }

    public String describe() {
        return host + ":" + port + "/" + name;
    }

    /** Creates the database if needed, then tables, upgrades and seed data. */
    public void initialize() throws SQLException, IOException {
        try (Connection c = DriverManager.getConnection(url(""), user, password);
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + name
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        String schema = readResource("/schema.sql");
        try (Connection c = connect(); Statement st = c.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.isBlank()) {
                    st.execute(sql);
                }
            }
            ensureMenuCategory(c);
            seedMenu(c);
            seedRooms(c);
        }
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url(name), user, password);
    }

    /** Runs read-only work on a fresh connection. */
    public <T> T query(SqlWork<T> work) throws SQLException {
        try (Connection c = connect()) {
            return work.apply(c);
        }
    }

    /** Runs work in one transaction: committed on success, rolled back on any exception. */
    public <T> T inTransaction(SqlWork<T> work) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private String url(String database) {
        String path = database.isEmpty() ? "/" : "/" + database;
        return "jdbc:mysql://" + host + ":" + port + path + "?" + params;
    }

    private static String readResource(String path) throws IOException {
        try (InputStream in = Database.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Missing resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Adds the category column to databases created by an older version. */
    private static void ensureMenuCategory(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'menu_items' AND COLUMN_NAME = 'category'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE menu_items ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'FOOD'");
        }
    }

    /** Makes the menu table match MENU: names, prices and categories are replaced and stale items are hidden. */
    private static void seedMenu(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO menu_items (id, name, price, category, active) VALUES (?, ?, ?, ?, 1) "
                        + "ON DUPLICATE KEY UPDATE name = VALUES(name), price = VALUES(price), "
                        + "category = VALUES(category), active = 1")) {
            StringBuilder ids = new StringBuilder();
            for (String[] row : MENU) {
                ps.setInt(1, Integer.parseInt(row[0]));
                ps.setString(2, row[1]);
                ps.setBigDecimal(3, new BigDecimal(row[2]));
                ps.setString(4, row[3]);
                ps.addBatch();
                if (ids.length() > 0) {
                    ids.append(',');
                }
                ids.append(row[0]);
            }
            ps.executeBatch();
            try (Statement st = c.createStatement()) {
                st.executeUpdate("UPDATE menu_items SET active = 0 WHERE id NOT IN (" + ids + ")");
            }
        }
    }

    /** Creates the 60 rooms the first time the app runs: 1-10, 11-30, 31-40, 41-60. */
    private static void seedRooms(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM rooms")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO rooms (room_number, type_code) VALUES (?, ?)")) {
            for (int n = 1; n <= 60; n++) {
                ps.setInt(1, n);
                ps.setString(2, typeCodeFor(n));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static String typeCodeFor(int roomNumber) {
        if (roomNumber <= 10) return "LUXURY_DOUBLE";
        if (roomNumber <= 30) return "DELUXE_DOUBLE";
        if (roomNumber <= 40) return "LUXURY_SINGLE";
        return "DELUXE_SINGLE";
    }
}
