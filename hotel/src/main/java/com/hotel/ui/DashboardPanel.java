package com.hotel.ui;

import com.hotel.model.Booking;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.service.HotelService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

/** Floor plan of all rooms, colour-coded by status, with quick actions for the selected room. */
final class DashboardPanel extends JPanel implements Screen {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final HotelService service;
    private final BiConsumer<String, Integer> navigate;
    private final JLabel totalValue = bigValue();
    private final JLabel freeValue = bigValue();
    private final JLabel busyValue = bigValue();
    private final JPanel floor = new JPanel();
    private final JTextArea details = new JTextArea();
    private final JButton checkinBtn = new JButton("Check in guest");
    private final JButton foodBtn = new JButton("Order food");
    private final JButton checkoutBtn = new JButton("Checkout & bill");
    private final Map<Integer, JPanel> tiles = new HashMap<>();
    private List<Room> rooms = List.of();
    private Room selected;

    DashboardPanel(HotelService service, BiConsumer<String, Integer> navigate) {
        this.service = service;
        this.navigate = navigate;
        setLayout(new BorderLayout(18, 0));
        setBackground(UiSupport.BG);
        setBorder(UiSupport.pad(20));

        // Left: header, stats and floor plan.
        JPanel left = UiSupport.borderPanel();
        JPanel head = UiSupport.borderPanel();
        head.add(UiSupport.title("Dashboard"), BorderLayout.WEST);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        head.add(refresh, BorderLayout.EAST);

        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.add(statCard("Total rooms", totalValue));
        stats.add(statCard("Free", freeValue));
        stats.add(statCard("Occupied", busyValue));

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(head, BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);
        left.add(top, BorderLayout.NORTH);

        floor.setLayout(new BoxLayout(floor, BoxLayout.Y_AXIS));
        floor.setOpaque(false);
        JScrollPane scroll = new JScrollPane(floor);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        left.add(scroll, BorderLayout.CENTER);
        add(left, BorderLayout.CENTER);

        // Right: details of the selected room and quick actions.
        JPanel side = UiSupport.card();
        side.setPreferredSize(new Dimension(300, 0));
        details.setEditable(false);
        details.setOpaque(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        side.add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(3, 1, 0, 8));
        actions.setOpaque(false);
        checkinBtn.addActionListener(e -> navigate.accept("checkin", selected.number()));
        foodBtn.addActionListener(e -> navigate.accept("food", selected.number()));
        checkoutBtn.addActionListener(e -> navigate.accept("checkout", selected.number()));
        actions.add(checkinBtn);
        actions.add(foodBtn);
        actions.add(checkoutBtn);
        side.add(actions, BorderLayout.SOUTH);
        add(side, BorderLayout.EAST);

        updateDetails();
    }

    @Override
    public void refresh() {
        UiSupport.background(this, service::rooms, loaded -> {
            rooms = loaded;
            render();
        });
    }

    private void render() {
        long busy = rooms.stream().filter(Room::occupied).count();
        totalValue.setText(String.valueOf(rooms.size()));
        busyValue.setText(String.valueOf(busy));
        freeValue.setText(String.valueOf(rooms.size() - busy));

        floor.removeAll();
        tiles.clear();
        Map<RoomType, List<Room>> byType = new LinkedHashMap<>();
        for (Room r : rooms) {
            byType.computeIfAbsent(r.type(), k -> new java.util.ArrayList<>()).add(r);
        }
        for (Map.Entry<RoomType, List<Room>> group : byType.entrySet()) {
            RoomType type = group.getKey();
            JLabel heading = UiSupport.sectionHeading(type.name() + "   |   "
                    + UiSupport.money(type.pricePerDay()) + " per day   |   "
                    + type.capacity() + (type.capacity() == 1 ? " guest" : " guests"));
            heading.setAlignmentX(LEFT_ALIGNMENT);

            JPanel grid = new JPanel(new GridLayout(0, 10, 8, 8));
            grid.setOpaque(false);
            grid.setAlignmentX(LEFT_ALIGNMENT);
            for (Room r : group.getValue()) {
                JPanel tile = createTile(r);
                tiles.put(r.number(), tile);
                grid.add(tile);
            }
            floor.add(heading);
            floor.add(Box.createVerticalStrut(8));
            floor.add(grid);
            floor.add(Box.createVerticalStrut(22));
        }

        // Keep the selected room (now with fresh data) highlighted.
        if (selected != null) {
            selected = rooms.stream().filter(r -> r.number() == selected.number()).findFirst().orElse(null);
        }
        updateTileBorders();
        updateDetails();
        floor.revalidate();
        floor.repaint();
    }

    private JPanel createTile(Room r) {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setPreferredSize(new Dimension(92, 66));
        tile.setBackground(r.occupied() ? UiSupport.BUSY_BG : UiSupport.FREE_BG);

        JLabel number = new JLabel(String.valueOf(r.number()), JLabel.CENTER);
        number.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        JLabel status = new JLabel(r.occupied() ? shortName(r.activeBooking()) : "Free", JLabel.CENTER);
        status.setFont(status.getFont().deriveFont(11f));
        tile.add(number, BorderLayout.CENTER);
        tile.add(status, BorderLayout.SOUTH);

        tile.setToolTipText(r.label());
        tile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                select(r);
                if (e.getClickCount() == 2) {
                    navigate.accept(r.occupied() ? "food" : "checkin", r.number());
                }
            }
        });
        return tile;
    }

    private void select(Room r) {
        selected = r;
        updateTileBorders();
        updateDetails();
    }

    private void updateTileBorders() {
        for (Room r : rooms) {
            JPanel tile = tiles.get(r.number());
            if (tile == null) {
                continue;
            }
            boolean isSelected = selected != null && selected.number() == r.number();
            Color borderColor = r.occupied() ? UiSupport.BUSY_BORDER : UiSupport.FREE_BORDER;
            Border line = BorderFactory.createLineBorder(borderColor, isSelected ? 3 : 1);
            tile.setBorder(BorderFactory.createCompoundBorder(line, BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        }
    }

    private void updateDetails() {
        if (selected == null) {
            details.setText("Select a room to see its details.\n\n"
                    + "Double-click a free room to check a guest in,\n"
                    + "or a busy room to order food.");
            checkinBtn.setEnabled(false);
            foodBtn.setEnabled(false);
            checkoutBtn.setEnabled(false);
            return;
        }
        Room r = selected;
        StringBuilder sb = new StringBuilder();
        sb.append("ROOM ").append(r.number()).append('\n');
        sb.append("-".repeat(28)).append('\n');
        sb.append(String.format("%-11s %s%n", "Type", r.type().name()));
        sb.append(String.format("%-11s %s%n", "Rate", UiSupport.money(r.type().pricePerDay()) + "/day"));
        sb.append(String.format("%-11s %s%n", "Guests", r.type().capacity()));
        sb.append(String.format("%-11s %s%n", "Status", r.occupied() ? "OCCUPIED" : "FREE"));
        sb.append(String.format("%-11s %s%n", "Features", Objects.toString(r.type().description(), "-")));
        if (r.occupied()) {
            Booking b = r.activeBooking();
            sb.append('\n').append("Checked in\n  ").append(DATE.format(b.checkIn())).append('\n');
            sb.append("\nGuest 1\n  ").append(b.guest1().name()).append('\n');
            sb.append("  ").append(b.guest1().contact()).append(", ").append(b.guest1().gender()).append('\n');
            if (b.guest2() != null) {
                sb.append("\nGuest 2\n  ").append(b.guest2().name()).append('\n');
                sb.append("  ").append(b.guest2().contact()).append(", ").append(b.guest2().gender()).append('\n');
            }
        }
        details.setText(sb.toString());
        checkinBtn.setEnabled(!r.occupied());
        foodBtn.setEnabled(r.occupied());
        checkoutBtn.setEnabled(r.occupied());
    }

    private static String shortName(Booking b) {
        String name = b.guest1().name();
        return name.length() > 10 ? name.substring(0, 9) + "." : name;
    }

    private static JLabel bigValue() {
        JLabel l = new JLabel("-");
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        return l;
    }

    private static JPanel statCard(String caption, JLabel value) {
        JPanel p = UiSupport.card();
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiSupport.LINE), BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        p.add(UiSupport.muted(caption), BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }
}
