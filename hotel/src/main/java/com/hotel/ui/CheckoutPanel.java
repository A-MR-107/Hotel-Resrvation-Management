package com.hotel.ui;

import com.hotel.billing.BillFormatter;
import com.hotel.model.Bill;
import com.hotel.model.Room;
import com.hotel.service.HotelService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Previews a room's bill, closes the booking, and opens the bill window for printing. */
final class CheckoutPanel extends JPanel implements Screen {

    private final HotelService service;
    private final RoomPicker roomPicker;
    private final JTextArea preview = new JTextArea();
    private final PillButton checkoutBtn = new PillButton("Checkout & open bill");
    private final PillButton billWindowBtn = new PillButton("Open bill window", PillButton.Style.SECONDARY);
    private Bill currentBill;
    private Integer wantedRoom;

    CheckoutPanel(HotelService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(UiSupport.pad(4));
        add(UiSupport.title("Checkout & bill"), BorderLayout.NORTH);

        roomPicker = new RoomPicker("Checked-in rooms", Room::occupied, this::onRoomPicked);
        JPanel left = UiSupport.card();
        left.setLayout(new BorderLayout());
        left.setPreferredSize(new Dimension(430, 0));
        left.add(roomPicker, BorderLayout.CENTER);

        preview.setEditable(false);
        preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        preview.setMargin(new Insets(10, 12, 10, 12));
        preview.setText("Choose a checked-in room to preview its bill.");
        JScrollPane scroll = new JScrollPane(preview);
        scroll.setBorder(BorderFactory.createLineBorder(UiSupport.LINE));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(billWindowBtn);
        buttons.add(checkoutBtn);

        JPanel right = UiSupport.card();
        right.setLayout(new BorderLayout(0, 14));
        right.add(UiSupport.sectionHeading("Bill preview"), BorderLayout.NORTH);
        right.add(scroll, BorderLayout.CENTER);
        right.add(buttons, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(left, BorderLayout.WEST);
        body.add(right, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        billWindowBtn.addActionListener(e -> openBillWindow());
        checkoutBtn.addActionListener(e -> checkout());
        updateButtons();
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
        });
    }

    private void onRoomPicked(Room room) {
        currentBill = null;
        if (room == null) {
            preview.setText("Choose a checked-in room to preview its bill.");
        } else {
            loadPreview(room);
        }
        updateButtons();
    }

    private void loadPreview(Room room) {
        UiSupport.background(this, () -> service.previewBill(room.number()), bill -> {
            Room current = roomPicker.selected();
            if (current != null && current.number() == room.number()) {
                show(bill);
            }
        });
    }

    private void show(Bill bill) {
        currentBill = bill;
        preview.setText(BillFormatter.toText(bill));
        preview.setCaretPosition(0);
        updateButtons();
    }

    private void checkout() {
        Room room = roomPicker.selected();
        if (room == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Check out room " + room.number() + " (" + room.activeBooking().guest1().name()
                        + ")?\nThe booking will be closed.",
                "Confirm checkout", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        UiSupport.background(this, () -> service.checkOut(room.number()), bill -> {
            show(bill);
            new BillWindow(this, bill).setVisible(true);
            refresh();
        });
    }

    private void openBillWindow() {
        if (currentBill != null) {
            new BillWindow(this, currentBill).setVisible(true);
        }
    }

    private void updateButtons() {
        checkoutBtn.setEnabled(roomPicker.selected() != null);
        billWindowBtn.setEnabled(currentBill != null);
    }
}
