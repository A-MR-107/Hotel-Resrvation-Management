package com.hotel.ui;

import com.hotel.model.Booking;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.service.HotelService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

/** Floor plan of all rooms, colour-coded by status, with quick actions for the selected room. */
final class DashboardPanel extends JPanel implements Screen {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final Font VALUE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
    private static final Font KEY_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    private final HotelService service;
    private final BiConsumer<String, Integer> navigate;
    private final JLabel totalValue = bigValue();
    private final JLabel freeValue = bigValue();
    private final JLabel busyValue = bigValue();
    private final JPanel floor = new JPanel();
    private final JLabel detailTitle = new JLabel("No room selected");
    private final JPanel detailBody = new JPanel(new GridBagLayout());
    private final PillButton checkinBtn = new PillButton("Check in guest");
    private final PillButton foodBtn = new PillButton("Order food");
    private final PillButton checkoutBtn = new PillButton("Checkout & bill");
    private final Map<Integer, JPanel> tiles = new HashMap<>();
    private List<Room> rooms = List.of();
    private Room selected;

    DashboardPanel(HotelService service, BiConsumer<String, Integer> navigate) {
        this.service = service;
        this.navigate = navigate;
        setLayout(new BorderLayout(18, 0));
        setOpaque(false);
        setBorder(UiSupport.pad(4));

        JPanel left = UiSupport.borderPanel();
        JPanel head = UiSupport.borderPanel();
        head.add(UiSupport.title("Dashboard"), BorderLayout.WEST);
        PillButton refresh = new PillButton("Refresh", PillButton.Style.SECONDARY);
        refresh.addActionListener(e -> refresh());
        head.add(refresh, BorderLayout.EAST);

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
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
        side.setLayout(new BorderLayout(0, 16));
        side.setPreferredSize(new Dimension(320, 0));
        detailTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        detailTitle.setForeground(UiSupport.ACCENT);
        JPanel bodyWrap = new JPanel(new BorderLayout());
        bodyWrap.setOpaque(false);
        bodyWrap.add(detailBody, BorderLayout.NORTH);
        JPanel middle = new JPanel(new BorderLayout(0, 12));
        middle.setOpaque(false);
        middle.add(detailTitle, BorderLayout.NORTH);
        middle.add(bodyWrap, BorderLayout.CENTER);
        side.add(middle, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(3, 1, 0, 10));
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
            byType.computeIfAbsent(r.type(), k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<RoomType, List<Room>> group : byType.entrySet()) {
            RoomType type = group.getKey();
            JLabel heading = UiSupport.sectionHeading(type.name() + "   |   "
                    + UiSupport.money(type.pricePerDay()) + " per day   |   "
                    + type.capacity() + (type.capacity() == 1 ? " guest" : " guests"));
            heading.setAlignmentX(LEFT_ALIGNMENT);

            JPanel grid = new JPanel(new GridLayout(0, 10, 10, 10));
            grid.setOpaque(false);
            grid.setAlignmentX(LEFT_ALIGNMENT);
            for (Room r : group.getValue()) {
                JPanel tile = createTile(r);
                tiles.put(r.number(), tile);
                grid.add(tile);
            }
            floor.add(heading);
            floor.add(Box.createVerticalStrut(10));
            floor.add(grid);
            floor.add(Box.createVerticalStrut(24));
        }

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
        tile.setPreferredSize(new Dimension(92, 70));
        tile.setBackground(r.occupied() ? UiSupport.BUSY_BG : UiSupport.FREE_BG);

        JLabel number = new JLabel(String.valueOf(r.number()), JLabel.CENTER);
        number.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        number.setForeground(UiSupport.TEXT);
        JLabel status = new JLabel(r.occupied() ? shortName(r.activeBooking()) : "Free", JLabel.CENTER);
        status.setFont(status.getFont().deriveFont(11f));
        status.setForeground(r.occupied() ? UiSupport.BUSY_TEXT : UiSupport.FREE_TEXT);
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
            Border line = BorderFactory.createLineBorder(isSelected ? UiSupport.ACCENT_HI : borderColor,
                    isSelected ? 3 : 1);
            tile.setBorder(BorderFactory.createCompoundBorder(line, BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        }
    }

    private void updateDetails() {
        detailBody.removeAll();
        if (selected == null) {
            detailTitle.setText("No room selected");
            addText(0, "Select a room to see its details.");
            addText(1, "Double-click a free room to check a guest in, or a busy room to order food.");
            checkinBtn.setEnabled(false);
            foodBtn.setEnabled(false);
            checkoutBtn.setEnabled(false);
        } else {
            Room r = selected;
            detailTitle.setText("Room " + r.number());
            int row = 0;
            row = addField(row, "Status", r.occupied() ? "Occupied" : "Free");
            row = addField(row, "Room type", r.type().name());
            row = addField(row, "Rate", UiSupport.money(r.type().pricePerDay()) + " per day");
            row = addField(row, "Guests allowed", String.valueOf(r.type().capacity()));
            row = addField(row, "Features", Objects.toString(r.type().description(), "-"));
            if (r.occupied()) {
                Booking b = r.activeBooking();
                row = addField(row, "Checked in", DATE.format(b.checkIn()));
                row = addField(row, "Guest 1", b.guest1().name() + "\n"
                        + b.guest1().contact() + ", " + b.guest1().gender());
                if (b.guest2() != null) {
                    row = addField(row, "Guest 2", b.guest2().name() + "\n"
                            + b.guest2().contact() + ", " + b.guest2().gender());
                }
            }
            checkinBtn.setEnabled(!r.occupied());
            foodBtn.setEnabled(r.occupied());
            checkoutBtn.setEnabled(r.occupied());
        }
        detailBody.revalidate();
        detailBody.repaint();
    }

    /** Adds a label above its value. Long values wrap inside the panel. */
    private int addField(int row, String key, String value) {
        JLabel k = new JLabel(key.toUpperCase());
        k.setFont(KEY_FONT);
        k.setForeground(UiSupport.MUTED);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(10, 0, 2, 0);
        detailBody.add(k, c);
        addText(row + 1, value);
        return row + 2;
    }

    private void addText(int row, String value) {
        String html = "<html><body style='width: 260px'>"
                + escape(value).replace("\n", "<br>") + "</body></html>";
        JLabel v = new JLabel(html);
        v.setFont(VALUE_FONT);
        v.setForeground(UiSupport.TEXT);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        detailBody.add(v, c);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String shortName(Booking b) {
        String name = b.guest1().name();
        return name.length() > 10 ? name.substring(0, 9) + "." : name;
    }

    private static JLabel bigValue() {
        JLabel l = new JLabel("-");
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        l.setForeground(UiSupport.TEXT);
        return l;
    }

    private static JPanel statCard(String caption, JLabel value) {
        JPanel p = UiSupport.card();
        p.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        p.add(UiSupport.muted(caption), BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }
}
