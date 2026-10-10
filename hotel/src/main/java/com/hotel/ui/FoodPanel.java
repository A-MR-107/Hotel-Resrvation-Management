package com.hotel.ui;

import com.hotel.model.Bill;
import com.hotel.model.FoodLine;
import com.hotel.model.MenuItem;
import com.hotel.model.Room;
import com.hotel.service.HotelService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/** Adds menu items to a checked-in room and shows what the room has ordered so far. */
final class FoodPanel extends JPanel implements Screen {

    private final HotelService service;
    private final JComboBox<Room> roomBox = new JComboBox<>();
    private final JComboBox<MenuItem> itemBox = new JComboBox<>();
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, HotelService.MAX_QUANTITY, 1));
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Item", "Qty", "Unit price", "Amount"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel totalLabel = new JLabel(" ");
    private final JLabel hint = UiSupport.muted(" ");
    private final JButton addBtn = new JButton("Add to order");
    private Integer wantedRoom;

    FoodPanel(HotelService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBackground(UiSupport.BG);
        setBorder(UiSupport.pad(20));

        JPanel card = UiSupport.card();
        card.setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(UiSupport.title("Food orders"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        UiSupport.row(form, 0, "Room", roomBox);
        UiSupport.row(form, 1, "Item", itemBox);
        UiSupport.row(form, 2, "Quantity", quantity);

        JPanel formRow = new JPanel(new BorderLayout());
        formRow.setBackground(Color.WHITE);
        formRow.add(form, BorderLayout.CENTER);
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addRow.setBackground(Color.WHITE);
        addRow.add(addBtn);
        formRow.add(addRow, BorderLayout.SOUTH);
        top.add(formRow, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Orders for this room"));
        card.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.add(hint, BorderLayout.WEST);
        totalLabel.setFont(totalLabel.getFont().deriveFont(java.awt.Font.BOLD, 15f));
        bottom.add(totalLabel, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);

        addBtn.addActionListener(e -> addOrder());
        roomBox.addActionListener(e -> loadBill((Room) roomBox.getSelectedItem()));
        UiSupport.background(this, service::menu, menu -> {
            itemBox.removeAllItems();
            menu.forEach(itemBox::addItem);
        });
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
            boolean any = roomBox.getItemCount() > 0;
            hint.setText(any ? " " : "No guests are checked in.");
            addBtn.setEnabled(any);
            if (!any) {
                clearTable();
            }
        });
    }

    private void addOrder() {
        Room room = (Room) roomBox.getSelectedItem();
        MenuItem item = (MenuItem) itemBox.getSelectedItem();
        if (room == null || item == null) {
            UiSupport.info(this, "Choose a room and a menu item.");
            return;
        }
        int qty = (Integer) quantity.getValue();
        UiSupport.background(this, () -> service.addFood(room.number(), item.id(), qty), line -> {
            hint.setText("Added " + qty + " x " + line.itemName() + " to room " + room.number() + ".");
            loadBill(room);
        });
    }

    private void loadBill(Room room) {
        if (room == null) {
            clearTable();
            return;
        }
        UiSupport.background(this, () -> service.previewBill(room.number()), bill -> {
            if (roomBox.getSelectedItem() == room) {
                showLines(bill);
            }
        });
    }

    private void showLines(Bill bill) {
        tableModel.setRowCount(0);
        for (FoodLine line : bill.foodLines()) {
            tableModel.addRow(new Object[]{
                    line.itemName(), line.quantity(), UiSupport.money(line.unitPrice()),
                    UiSupport.money(line.lineTotal())});
        }
        totalLabel.setText("Food total: " + UiSupport.money(bill.foodTotal()));
    }

    private void clearTable() {
        tableModel.setRowCount(0);
        totalLabel.setText(" ");
    }
}
