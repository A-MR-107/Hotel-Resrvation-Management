package com.hotel.ui;

import com.hotel.model.Guest;
import com.hotel.model.Room;
import com.hotel.service.HotelService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Books a free room for one or two guests. */
final class CheckinPanel extends JPanel implements Screen {

    private final HotelService service;
    private final JComboBox<Room> roomBox = new JComboBox<>();
    private final JTextField name1 = new JTextField(24);
    private final JTextField contact1 = new JTextField(24);
    private final JComboBox<String> gender1 = new JComboBox<>(HotelService.genders().toArray(String[]::new));
    private final JTextField name2 = new JTextField(24);
    private final JTextField contact2 = new JTextField(24);
    private final JComboBox<String> gender2 = new JComboBox<>(HotelService.genders().toArray(String[]::new));
    private final JPanel guest2Section = new JPanel(new GridBagLayout());
    private final JLabel hint = UiSupport.muted(" ");
    private final JButton checkInBtn = new JButton("Check in");
    private Integer wantedRoom;

    CheckinPanel(HotelService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBackground(UiSupport.BG);
        setBorder(UiSupport.pad(20));

        JPanel card = UiSupport.card();
        card.setLayout(new BorderLayout(0, 16));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(java.awt.Color.WHITE);
        UiSupport.row(form, 0, "Room", roomBox);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(18, 0, 4, 0);
        form.add(UiSupport.sectionHeading("Guest 1"), c);
        UiSupport.row(form, 2, "Full name", name1);
        UiSupport.row(form, 3, "Contact number", contact1);
        UiSupport.row(form, 4, "Gender", gender1);

        guest2Section.setBackground(java.awt.Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(18, 0, 4, 0);
        guest2Section.add(UiSupport.sectionHeading("Guest 2 (double room)"), g);
        UiSupport.row(guest2Section, 1, "Full name", name2);
        UiSupport.row(guest2Section, 2, "Contact number", contact2);
        UiSupport.row(guest2Section, 3, "Gender", gender2);

        GridBagConstraints gs = new GridBagConstraints();
        gs.gridx = 0;
        gs.gridy = 5;
        gs.gridwidth = 2;
        gs.fill = GridBagConstraints.HORIZONTAL;
        gs.weightx = 1;
        form.add(guest2Section, gs);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(java.awt.Color.WHITE);
        top.add(UiSupport.title("Check-in"), BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        card.add(top, BorderLayout.CENTER);

        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> clearForm());
        checkInBtn.addActionListener(e -> checkIn());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(java.awt.Color.WHITE);
        bottom.add(hint, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(java.awt.Color.WHITE);
        buttons.add(clear);
        buttons.add(checkInBtn);
        bottom.add(buttons, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);

        add(card, BorderLayout.NORTH);

        roomBox.addActionListener(e -> updateGuestTwo());
        updateGuestTwo();
    }

    @Override
    public void focusRoom(int roomNumber) {
        wantedRoom = roomNumber;
    }

    @Override
    public void refresh() {
        UiSupport.background(this, service::rooms, rooms -> {
            UiSupport.fillRooms(roomBox, rooms, r -> !r.occupied(), wantedRoom);
            wantedRoom = null;
            hint.setText(roomBox.getItemCount() == 0 ? "No free rooms right now." : " ");
            updateGuestTwo();
        });
    }

    private void updateGuestTwo() {
        Room room = (Room) roomBox.getSelectedItem();
        boolean two = room != null && room.type().twoGuests();
        setEnabledRecursive(guest2Section, two);
        guest2Section.setVisible(two);
        checkInBtn.setEnabled(room != null);
        revalidate();
        repaint();
    }

    private void checkIn() {
        Room room = (Room) roomBox.getSelectedItem();
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
