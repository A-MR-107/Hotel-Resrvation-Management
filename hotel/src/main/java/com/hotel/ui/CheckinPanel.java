package com.hotel.ui;

import com.hotel.model.Guest;
import com.hotel.model.Room;
import com.hotel.service.HotelService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Books a free room for one or two guests. Rooms are chosen by room type. */
final class CheckinPanel extends JPanel implements Screen {

    private final HotelService service;
    private final RoomPicker roomPicker;
    private final JLabel chosenLabel = UiSupport.sectionHeading("No room selected");
    private final JTextField name1 = new JTextField(22);
    private final JTextField contact1 = new JTextField(22);
    private final JComboBox<String> gender1 = new JComboBox<>(HotelService.genders().toArray(String[]::new));
    private final JTextField name2 = new JTextField(22);
    private final JTextField contact2 = new JTextField(22);
    private final JComboBox<String> gender2 = new JComboBox<>(HotelService.genders().toArray(String[]::new));
    private final JPanel guest2Section = new JPanel(new GridBagLayout());
    private final JLabel hint = UiSupport.muted(" ");
    private final PillButton checkInBtn = new PillButton("Check in");
    private Integer wantedRoom;

    CheckinPanel(HotelService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(UiSupport.pad(4));
        add(UiSupport.title("Check-in"), BorderLayout.NORTH);

        roomPicker = new RoomPicker("Free rooms", r -> !r.occupied(), this::onRoomPicked);
        JPanel left = UiSupport.card();
        left.setLayout(new BorderLayout());
        left.setPreferredSize(new Dimension(430, 0));
        left.add(roomPicker, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.add(UiSupport.sectionHeading("Guest 1"), heading(0));
        UiSupport.row(form, 1, "Full name", name1);
        UiSupport.row(form, 2, "Contact number", contact1);
        UiSupport.row(form, 3, "Gender", gender1);

        guest2Section.setOpaque(false);
        guest2Section.add(UiSupport.sectionHeading("Guest 2 (double room)"), heading(0));
        UiSupport.row(guest2Section, 1, "Full name", name2);
        UiSupport.row(guest2Section, 2, "Contact number", contact2);
        UiSupport.row(guest2Section, 3, "Gender", gender2);

        GridBagConstraints gs = new GridBagConstraints();
        gs.gridx = 0;
        gs.gridy = 4;
        gs.gridwidth = 2;
        gs.fill = GridBagConstraints.HORIZONTAL;
        gs.weightx = 1;
        form.add(guest2Section, gs);

        for (java.awt.Component f : new java.awt.Component[]{name1, contact1, gender1, name2, contact2, gender2}) {
            UiSupport.enterMovesNext((javax.swing.JComponent) f);
        }

        PillButton clear = new PillButton("Clear", PillButton.Style.SECONDARY);
        clear.addActionListener(e -> clearForm());
        checkInBtn.addActionListener(e -> checkIn());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(clear);
        buttons.add(checkInBtn);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(hint, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        JPanel right = UiSupport.card();
        right.setLayout(new BorderLayout(0, 16));
        right.add(chosenLabel, BorderLayout.NORTH);
        right.add(form, BorderLayout.CENTER);
        right.add(bottom, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(left, BorderLayout.WEST);
        body.add(right, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        updateGuestTwo(null);
    }

    private static GridBagConstraints heading(int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 0, 4, 0);
        return c;
    }

    @Override
    public void focusRoom(int roomNumber) {
        wantedRoom = roomNumber;
    }

    @Override
    public void refresh() {
        UiSupport.background(this, service::rooms, rooms -> {
            roomPicker.setRooms(rooms, wantedRoom);
            wantedRoom = null;
            hint.setText(rooms.stream().anyMatch(r -> !r.occupied()) ? " " : "No free rooms right now.");
            updateGuestTwo(roomPicker.selected());
        });
    }

    private void onRoomPicked(Room room) {
        updateGuestTwo(room);
    }

    private void updateGuestTwo(Room room) {
        boolean two = room != null && room.type().twoGuests();
        setEnabledRecursive(guest2Section, two);
        guest2Section.setVisible(two);
        checkInBtn.setEnabled(room != null);
        chosenLabel.setText(room == null ? "No room selected"
                : "Room " + room.number() + "  -  " + room.type().name());
        revalidate();
        repaint();
    }

    private void checkIn() {
        Room room = roomPicker.selected();
        if (room == null) {
            UiSupport.info(this, "Choose a free room first.");
            return;
        }
        Guest g1 = new Guest(name1.getText().trim(), contact1.getText().trim(),
                (String) gender1.getSelectedItem());
        Guest g2 = room.type().twoGuests()
                ? new Guest(name2.getText().trim(), contact2.getText().trim(), (String) gender2.getSelectedItem())
                : null;
        UiSupport.background(this, () -> service.checkIn(room.number(), g1, g2), booking -> {
            UiSupport.info(this, "Room " + room.number() + " is booked for " + g1.name() + ".");
            clearForm();
            refresh();
        });
    }

    private void clearForm() {
        name1.setText("");
        contact1.setText("");
        name2.setText("");
        contact2.setText("");
        gender1.setSelectedIndex(0);
        gender2.setSelectedIndex(0);
    }

    private static void setEnabledRecursive(JPanel panel, boolean enabled) {
        for (java.awt.Component comp : panel.getComponents()) {
            comp.setEnabled(enabled);
        }
    }
}
