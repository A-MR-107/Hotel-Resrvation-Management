package com.hotel.ui;

import com.hotel.model.Bill;
import com.hotel.model.Room;
import com.hotel.billing.BillFormatter;
import com.hotel.service.HotelService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Previews a room's bill, closes the booking, and opens the bill window for printing. */
final class CheckoutPanel extends JPanel implements Screen {

    private final HotelService service;
    private final JComboBox<Room> roomBox = new JComboBox<>();
    private final JTextArea preview = new JTextArea();
    private final JButton checkoutBtn = new JButton("Checkout & open bill");
    private final JButton billWindowBtn = new JButton("Open bill window");
    private final JLabel hint = UiSupport.muted(" ");
    private Bill currentBill;
    private Integer wantedRoom;

    CheckoutPanel(HotelService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBackground(UiSupport.BG);
        setBorder(UiSupport.pad(20));

        JPanel card = UiSupport.card();
        card.setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setBackground(Color.WHITE);
        top.add(UiSupport.title("Checkout & bill"), BorderLayout.NORTH);
        JPanel pick = new JPanel(new BorderLayout(10, 0));
        pick.setBackground(Color.WHITE);
        pick.add(new JLabel("Room"), BorderLayout.WEST);
        pick.add(roomBox, BorderLayout.CENTER);
        top.add(pick, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);

        preview.setEditable(false);
        preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        preview.setMargin(new java.awt.Insets(10, 12, 10, 12));
        preview.setText("Select a checked-in room to preview its bill.");
        JScrollPane scroll = new JScrollPane(preview);
        scroll.setBorder(BorderFactory.createLineBorder(UiSupport.LINE));
        card.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.add(hint, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(billWindowBtn);
        buttons.add(checkoutBtn);
        bottom.add(buttons, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);

        roomBox.addActionListener(e -> onRoomChanged());
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
            UiSupport.fillRooms(roomBox, rooms, Room::occupied, wantedRoom);
            wantedRoom = null;
            hint.setText(roomBox.getItemCount() == 0 ? "No guests are checked in." : " ");
            updateButtons();
        });
    }

    private void onRoomChanged() {
        Room room = (Room) roomBox.getSelectedItem();
        currentBill = null;
        if (room == null) {
            preview.setText("Select a checked-in room to preview its bill.");
        } else {
            loadPreview(room);
        }
        updateButtons();
    }

    private void loadPreview(Room room) {
        UiSupport.background(this, () -> service.previewBill(room.number()), bill -> {
            if (roomBox.getSelectedItem() == room) {
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
        Room room = (Room) roomBox.getSelectedItem();
        if (room == null) {
            return;
        }
        int answer = javax.swing.JOptionPane.showConfirmDialog(this,
                "Check out room " + room.number() + " (" + room.activeBooking().guest1().name()
                        + ")?\nThe booking will be closed.",
                "Confirm checkout", javax.swing.JOptionPane.OK_CANCEL_OPTION);
        if (answer != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }
        UiSupport.background(this, () -> service.checkOut(room.number()), bill -> {
            show(bill);
            BillWindow window = new BillWindow(this, bill);
            window.setVisible(true);
            refresh();
        });
    }

    private void openBillWindow() {
        if (currentBill != null) {
            new BillWindow(this, currentBill).setVisible(true);
        }
    }

    private void updateButtons() {
        boolean hasRoom = roomBox.getSelectedItem() != null;
        checkoutBtn.setEnabled(hasRoom);
        billWindowBtn.setEnabled(currentBill != null);
    }
}
