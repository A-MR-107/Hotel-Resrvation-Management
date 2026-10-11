package com.hotel.db;

import com.hotel.model.Booking;
import com.hotel.model.FoodLine;
import com.hotel.model.Guest;
import com.hotel.model.MenuItem;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** All SQL lives here. Methods take a connection so the service controls transactions. */
public final class HotelRepository {

    /**
     * Rooms with their type and the active booking (if any).
     * When lockForUpdate is true the rows are locked until the transaction ends,
     * which stops two people from booking the same room at once.
     */
    public List<Room> findRooms(Connection c, Integer roomNumber, boolean lockForUpdate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT r.room_number, t.code, t.name, t.price_per_day, t.capacity, t.description, "
                        + "b.id AS booking_id, b.guest1_name, b.guest1_contact, b.guest1_gender, "
                        + "b.guest2_name, b.guest2_contact, b.guest2_gender, b.check_in "
                        + "FROM rooms r "
                        + "JOIN room_types t ON t.code = r.type_code "
                        + "LEFT JOIN bookings b ON b.room_number = r.room_number AND b.status = 'ACTIVE' ");
        if (roomNumber != null) {
            sql.append("WHERE r.room_number = ? ");
        }
        sql.append("ORDER BY r.room_number");
        if (lockForUpdate) {
            sql.append(" FOR UPDATE");
        }

        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            if (roomNumber != null) {
                ps.setInt(1, roomNumber);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Room> rooms = new ArrayList<>();
                while (rs.next()) {
                    RoomType type = mapType(rs);
                    Booking booking = null;
                    long bookingId = rs.getLong("booking_id");
                    if (!rs.wasNull()) {
                        Guest g1 = new Guest(rs.getString("guest1_name"),
                                rs.getString("guest1_contact"), rs.getString("guest1_gender"));
                        String g2Name = rs.getString("guest2_name");
                        Guest g2 = g2Name == null ? null : new Guest(g2Name,
                                rs.getString("guest2_contact"), rs.getString("guest2_gender"));
                        booking = new Booking(bookingId, rs.getInt("room_number"), g1, g2,
                                rs.getTimestamp("check_in").toLocalDateTime());
                    }
                    rooms.add(new Room(rs.getInt("room_number"), type, booking));
                }
                return rooms;
            }
        }
    }

    public List<MenuItem> findMenu(Connection c) throws SQLException {
        List<MenuItem> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, name, price, category FROM menu_items WHERE active = 1 ORDER BY category DESC, name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new MenuItem(rs.getInt("id"), rs.getString("name"), rs.getBigDecimal("price"), rs.getString("category")));
            }
        }
        return items;
    }

    public MenuItem findMenuItem(Connection c, int itemId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, name, price, category FROM menu_items WHERE id = ? AND active = 1")) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new MenuItem(rs.getInt("id"), rs.getString("name"), rs.getBigDecimal("price"), rs.getString("category"));
            }
        }
    }

    public long insertBooking(Connection c, int roomNumber, Guest g1, Guest g2, LocalDateTime checkIn)
            throws SQLException {
        String sql = "INSERT INTO bookings (room_number, status, guest1_name, guest1_contact, guest1_gender, "
                + "guest2_name, guest2_contact, guest2_gender, check_in) VALUES (?, 'ACTIVE', ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, roomNumber);
            setGuest(ps, 2, g1);
            setGuest(ps, 5, g2);
            ps.setTimestamp(8, Timestamp.valueOf(checkIn));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertFoodOrder(Connection c, long bookingId, MenuItem item, int quantity,
                                BigDecimal lineTotal, LocalDateTime orderedAt) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO food_orders (booking_id, item_id, quantity, unit_price, line_total, ordered_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, bookingId);
            ps.setInt(2, item.id());
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, item.price());
            ps.setBigDecimal(5, lineTotal);
            ps.setTimestamp(6, Timestamp.valueOf(orderedAt));
            ps.executeUpdate();
        }
    }

    public List<FoodLine> findFoodLines(Connection c, long bookingId) throws SQLException {
        List<FoodLine> lines = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT m.name, f.quantity, f.unit_price, f.line_total "
                        + "FROM food_orders f JOIN menu_items m ON m.id = f.item_id "
                        + "WHERE f.booking_id = ? ORDER BY f.id")) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add(new FoodLine(rs.getString("name"), rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price"), rs.getBigDecimal("line_total")));
                }
            }
        }
        return lines;
    }

    public void closeBooking(Connection c, long bookingId, LocalDateTime checkOut, BigDecimal total)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE bookings SET status = 'CHECKED_OUT', check_out = ?, total_amount = ? "
                        + "WHERE id = ? AND status = 'ACTIVE'")) {
            ps.setTimestamp(1, Timestamp.valueOf(checkOut));
            ps.setBigDecimal(2, total);
            ps.setLong(3, bookingId);
            ps.executeUpdate();
        }
    }

    private static RoomType mapType(ResultSet rs) throws SQLException {
        return new RoomType(rs.getString("code"), rs.getString("name"), rs.getBigDecimal("price_per_day"),
                rs.getInt("capacity"), rs.getString("description"));
    }

    private static void setGuest(PreparedStatement ps, int from, Guest g) throws SQLException {
        if (g == null) {
            ps.setNull(from, Types.VARCHAR);
            ps.setNull(from + 1, Types.VARCHAR);
            ps.setNull(from + 2, Types.VARCHAR);
        } else {
            ps.setString(from, g.name());
            ps.setString(from + 1, g.contact());
            ps.setString(from + 2, g.gender());
        }
    }
}
