package com.hotel.billing;

import com.hotel.model.Bill;
import com.hotel.model.FoodLine;
import com.hotel.model.Guest;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Renders a bill as fixed-width text (used by the bill window, printing and saving). */
public final class BillFormatter {

    public static final int WIDTH = 52;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private BillFormatter() {
    }

    public static String money(BigDecimal amount) {
        return String.format(Locale.US, "Rs. %,.2f", amount);
    }

    public static String toText(Bill b) {
        String rule = "-".repeat(WIDTH) + "\n";
        String heavy = "=".repeat(WIDTH) + "\n";
        StringBuilder sb = new StringBuilder();

        sb.append(center("HOTEL MANAGEMENT SYSTEM")).append('\n');
        sb.append(center("Guest Bill")).append("\n\n");
        sb.append(rule);
        sb.append(pair("Bill no.", String.format("%06d", b.booking().id()))).append('\n');
        sb.append(pair("Room", b.room().number() + " - " + b.room().type().name())).append('\n');
        sb.append(pair("Check-in", DATE.format(b.booking().checkIn()))).append('\n');
        sb.append(pair("Check-out", DATE.format(b.checkOut()))).append('\n');
        sb.append(pair("Nights", String.valueOf(b.nights()))).append('\n');
        sb.append(rule);

        sb.append("Guest(s)\n");
        appendGuest(sb, 1, b.booking().guest1());
        if (b.booking().guest2() != null) {
            appendGuest(sb, 2, b.booking().guest2());
        }
        sb.append(rule);

        String perDay = money(b.room().type().pricePerDay());
        sb.append(pair("Room charge (" + b.nights() + " x " + perDay + ")", money(b.roomCharge()))).append('\n');
        sb.append(rule);

        sb.append("Food & beverages\n");
        if (b.foodLines().isEmpty()) {
            sb.append("  (no orders)\n");
        } else {
            sb.append(String.format("%-24s %4s %10s %10s%n", "Item", "Qty", "Price", "Amount"));
            for (FoodLine line : b.foodLines()) {
                sb.append(String.format("%-24s %4d %10s %10s%n",
                        truncate(line.itemName(), 24), line.quantity(),
                        plain(line.unitPrice()), plain(line.lineTotal())));
            }
        }
        sb.append(rule);

        sb.append(pair("Room charge", money(b.roomCharge()))).append('\n');
        sb.append(pair("Food & beverages", money(b.foodTotal()))).append('\n');
        sb.append(heavy);
        sb.append(pair("GRAND TOTAL", money(b.grandTotal()))).append('\n');
        sb.append(heavy);
        sb.append('\n').append(center("Thank you for staying with us!")).append('\n');
        return sb.toString();
    }

    private static void appendGuest(StringBuilder sb, int index, Guest g) {
        sb.append(String.format(" %d. %s (%s, %s)%n", index, g.name(), g.contact(), g.gender()));
    }

    private static String pair(String left, String right) {
        int gap = WIDTH - left.length() - right.length();
        return left + " ".repeat(Math.max(1, gap)) + right;
    }

    private static String center(String text) {
        int pad = Math.max(0, (WIDTH - text.length()) / 2);
        return " ".repeat(pad) + text;
    }

    private static String plain(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
