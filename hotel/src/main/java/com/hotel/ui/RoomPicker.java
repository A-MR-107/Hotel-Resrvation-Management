package com.hotel.ui;

import com.hotel.model.Room;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Room chooser with no dropdown and no scroll bar: one button per room type,
 * then every room of that type as a tile.
 */
final class RoomPicker extends JPanel {

    private static final int COLUMNS = 5;

    private final Predicate<Room> filter;
    private final Consumer<Room> onSelect;
    private final JPanel typeGrid = new JPanel(new GridLayout(0, 2, 8, 8));
    private final JPanel chipGrid = new JPanel(new GridLayout(0, COLUMNS, 8, 8));
    private final JLabel summary = UiSupport.muted(" ");
    private final Map<String, PillButton> typeButtons = new LinkedHashMap<>();
    private final Map<String, List<Room>> roomsByType = new LinkedHashMap<>();
    private Room selected;

    RoomPicker(String heading, Predicate<Room> filter, Consumer<Room> onSelect) {
        this.filter = filter;
        this.onSelect = onSelect;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(UiSupport.sectionHeading(heading), BorderLayout.NORTH);
        typeGrid.setOpaque(false);
        top.add(typeGrid, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // The grid sits at the top of its area, so its height is always its real height.
        JPanel chipHolder = new JPanel(new BorderLayout());
        chipHolder.setOpaque(false);
        chipHolder.add(chipGrid, BorderLayout.NORTH);
        chipGrid.setOpaque(false);
        add(chipHolder, BorderLayout.CENTER);
        add(summary, BorderLayout.SOUTH);
    }

    /** Reloads the rooms, keeping the wanted (or current) room selected when it is still listed. */
    void setRooms(List<Room> rooms, Integer wantedRoom) {
        Room before = selected;
        Integer keep = wantedRoom != null ? wantedRoom : (before != null ? before.number() : null);

        roomsByType.clear();
        for (Room r : rooms) {
            if (filter.test(r)) {
                roomsByType.computeIfAbsent(r.type().code(), k -> new ArrayList<>()).add(r);
            }
        }
        selected = null;
        if (keep != null) {
            for (List<Room> list : roomsByType.values()) {
                for (Room r : list) {
                    if (r.number() == keep) {
                        selected = r;
                    }
                }
            }
        }

        typeGrid.removeAll();
        typeButtons.clear();
        for (Map.Entry<String, List<Room>> entry : roomsByType.entrySet()) {
            String code = entry.getKey();
            List<Room> list = entry.getValue();
            PillButton button = new PillButton(list.get(0).type().name() + " (" + list.size() + ")",
                    PillButton.Style.SECONDARY);
            button.addActionListener(ev -> showType(code));
            typeButtons.put(code, button);
            typeGrid.add(button);
        }
        String shown = selected != null ? selected.type().code()
                : (roomsByType.isEmpty() ? null : roomsByType.keySet().iterator().next());
        showType(shown);
        revalidate();
        repaint();

        if (!sameRoom(before, selected)) {
            onSelect.accept(selected);
        }
    }

    Room selected() {
        return selected;
    }

    private void showType(String code) {
        typeButtons.forEach((k, b) -> b.setActive(k.equals(code)));
        chipGrid.removeAll();
        if (code == null) {
            summary.setText("No rooms to show.");
        } else {
            for (Room r : roomsByType.get(code)) {
                Chip chip = new Chip(String.valueOf(r.number()), null,
                        r.occupied() ? UiSupport.BUSY_BG : UiSupport.FREE_BG,
                        r.occupied() ? UiSupport.BUSY_BORDER : UiSupport.FREE_BORDER, 76, 46);
                chip.setToolTipText(r.label());
                chip.setChosen(selected != null && selected.number() == r.number());
                chip.addActionListener(ev -> choose(r));
                chipGrid.add(chip);
            }
            summary.setText(selected == null ? "Tap a room number to select it."
                    : "Selected: " + selected.label());
        }
        chipGrid.revalidate();
        chipGrid.repaint();
    }

    private void choose(Room room) {
        Room before = selected;
        selected = room;
        showType(room.type().code());
        if (!sameRoom(before, selected)) {
            onSelect.accept(selected);
        }
    }

    private static boolean sameRoom(Room a, Room b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.number() == b.number();
    }
}
