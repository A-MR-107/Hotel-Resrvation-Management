package com.hotel.service;

import com.hotel.db.Database;
import com.hotel.db.HotelRepository;
import com.hotel.model.Bill;
import com.hotel.model.Booking;
import com.hotel.model.FoodLine;
import com.hotel.model.Guest;
import com.hotel.model.MenuItem;
import com.hotel.model.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Business rules: booking, food orders, billing and checkout.
 * The UI talks only to this class. Every method that changes data runs in a transaction.
 */
public final class HotelService {

    public static final int MAX_QUANTITY = 50;

    private static final Pattern CONTACT = Pattern.compile("[+0-9][0-9 \\-]{6,14}");
    private static final List<String> GENDERS = List.of("Male", "Female", "Other");

    private final Database db;
    private final HotelRepository repo = new HotelRepository();
    private final Clock clock;

    public HotelService(Database db) {
        this(db, Clock.systemDefaultZone());
    }

    public HotelService(Database db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    public static List<String> genders() {
        return GENDERS;
    }

    public List<Room> rooms() throws SQLException {
        return db.query(c -> repo.findRooms(c, null, false));
    }

    public List<MenuItem> menu() throws SQLException {
        return db.query(repo::findMenu);
    }

    /** Books a room. guest2 is required for double rooms and ignored for single rooms. */
    public Booking checkIn(int roomNumber, Guest guest1, Guest guest2) throws SQLException {
        validateGuest(guest1, "Guest 1");
        return db.inTransaction(c -> {
            Room room = findRoom(c, roomNumber);
            if (room.occupied()) {
                throw new HotelException("Room " + roomNumber + " is already booked.");
            }
            Guest second = null;
            if (room.type().twoGuests()) {
                validateGuest(guest2, "Guest 2");
                second = guest2;
            }
            LocalDateTime now = now();
            long id = repo.insertBooking(c, roomNumber, guest1, second, now);
            return new Booking(id, roomNumber, guest1, second, now);
        });
    }

    /** Adds a food item to the active booking of a room. */
    public FoodLine addFood(int roomNumber, int itemId, int quantity) throws SQLException {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new HotelException("Quantity must be between 1 and " + MAX_QUANTITY + ".");
        }
        return db.inTransaction(c -> {
            Room room = findRoom(c, roomNumber);
            requireOccupied(room);
            MenuItem item = repo.findMenuItem(c, itemId);
            if (item == null) {
                throw new HotelException("That menu item is no longer available.");
            }
            BigDecimal lineTotal = item.price().multiply(BigDecimal.valueOf(quantity));
            repo.insertFoodOrder(c, room.activeBooking().id(), item, quantity, lineTotal, now());
            return new FoodLine(item.name(), quantity, item.price(), lineTotal);
        });
    }

    /** Calculates the current bill without closing the booking. */
    public Bill previewBill(int roomNumber) throws SQLException {
        return db.query(c -> {
            Room room = findRoom(c, roomNumber);
            requireOccupied(room);
            return buildBill(c, room, now());
        });
    }

    /** Calculates the final bill and closes the booking. */
    public Bill checkOut(int roomNumber) throws SQLException {
        return db.inTransaction(c -> {
            Room room = findRoom(c, roomNumber);
            requireOccupied(room);
            LocalDateTime now = now();
            Bill bill = buildBill(c, room, now);
            repo.closeBooking(c, room.activeBooking().id(), now, bill.grandTotal());
            return bill;
        });
    }

    /**
     * Room charge = price per day x nights (minimum one night), plus food.
     * Pure function, so it can be tested without a database.
     */
    public static Bill calculateBill(Room room, Booking booking, List<FoodLine> foods, LocalDateTime checkOut) {
        long nights = Math.max(1,
                ChronoUnit.DAYS.between(booking.checkIn().toLocalDate(), checkOut.toLocalDate()));
        BigDecimal roomCharge = room.type().pricePerDay()
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal foodTotal = foods.stream()
                .map(FoodLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Bill(room, booking, nights, roomCharge, foods, foodTotal,
                roomCharge.add(foodTotal), checkOut);
    }

    private Bill buildBill(java.sql.Connection c, Room room, LocalDateTime checkOut) throws SQLException {
        List<FoodLine> foods = repo.findFoodLines(c, room.activeBooking().id());
        return calculateBill(room, room.activeBooking(), foods, checkOut);
    }

    private Room findRoom(Connection c, int roomNumber) throws SQLException {
        List<Room> rooms = repo.findRooms(c, roomNumber, true);
        if (rooms.isEmpty()) {
            throw new HotelException("Room " + roomNumber + " does not exist.");
        }
        return rooms.get(0);
    }

    private static void requireOccupied(Room room) {
        if (!room.occupied()) {
            throw new HotelException("Room " + room.number() + " has no active booking.");
        }
    }

    private static void validateGuest(Guest g, String label) {
        if (g == null) {
            throw new HotelException(label + " details are required.");
        }
        if (g.name() == null || g.name().isBlank()) {
            throw new HotelException(label + ": name is required.");
        }
        if (g.contact() == null || !CONTACT.matcher(g.contact().trim()).matches()) {
            throw new HotelException(label + ": enter a valid contact number (7-15 digits).");
        }
        if (!GENDERS.contains(g.gender())) {
            throw new HotelException(label + ": choose a gender.");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
