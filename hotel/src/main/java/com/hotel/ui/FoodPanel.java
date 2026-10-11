package com.hotel.ui;

import com.hotel.model.Bill;
import com.hotel.model.FoodLine;
import com.hotel.model.MenuItem;
import com.hotel.model.Room;
import com.hotel.service.HotelService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Orders food for a checked-in room: pick a room, tap items, choose quantity, add. */
final class FoodPanel extends JPanel implements Screen {

    private final HotelService service;
    private final RoomPicker roomPicker;
    private final MenuPicker menuPicker;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Item", "Qty", "Unit price", "Amount"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel totalLabel = new JLabel(" ");
    private final JLabel hint = UiSupport.muted(" ");
    private final JLabel chosenLabel = UiSupport.muted("Tap an item in the menu");
    private final JLabel qtyLabel = new JLabel("1", JLabel.CENTER);
    private final PillButton minus = new PillButton("-", PillButton.Style.SECONDARY);
    private final PillButton plus = new PillButton("+", PillButton.Style.SECONDARY);
    private final PillButton addBtn = new PillButton("Add to order");
    private Integer wantedRoom;
    private MenuItem chosenItem;
    private int quantity = 1;

    FoodPanel(HotelService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(UiSupport.pad(4));
        add(UiSupport.title("Food orders"), BorderLayout.NORTH);

        roomPicker = new RoomPicker("Checked-in rooms", Room::occupied, this::onRoomPicked);
        menuPicker = new MenuPicker(this::onItemPicked);

        JPanel left = UiSupport.card();
        left.setLayout(new BorderLayout(0, 16));
        left.setPreferredSize(new Dimension(430, 0));
        left.add(roomPicker, BorderLayout.NORTH);

        JPanel orders = new JPanel(new BorderLayout(0, 8));
        orders.setOpaque(false);
        orders.add(UiSupport.sectionHeading("Orders for this room"), BorderLayout.NORTH);
        JTable table = new JTable(tableModel);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiSupport.LINE));
        orders.add(scroll, BorderLayout.CENTER);
        JPanel ordersFooter = new JPanel(new BorderLayout());
        ordersFooter.setOpaque(false);
        ordersFooter.add(hint, BorderLayout.WEST);
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 16f));
        totalLabel.setForeground(UiSupport.ACCENT);
        ordersFooter.add(totalLabel, BorderLayout.EAST);
        orders.add(ordersFooter, BorderLayout.SOUTH);
        left.add(orders, BorderLayout.CENTER);

        JPanel right = UiSupport.card();
        right.setLayout(new BorderLayout(0, 16));
        right.add(UiSupport.sectionHeading("Menu"), BorderLayout.NORTH);
        right.add(menuPicker, BorderLayout.CENTER);
        right.add(footer(), BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(left, BorderLayout.WEST);
        body.add(right, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        addBtn.setEnabled(false);
        UiSupport.background(this, service::menu, menuPicker::setItems);
    }

    private JPanel footer() {
        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        footer.add(chosenLabel, BorderLayout.NORTH);

        JPanel stepper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        stepper.setOpaque(false);
        minus.setPreferredSize(new Dimension(44, 40));
        plus.setPreferredSize(new Dimension(44, 40));
        qtyLabel.setPreferredSize(new Dimension(48, 40));
        qtyLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        qtyLabel.setForeground(UiSupport.TEXT);
        minus.addActionListener(e -> changeQuantity(-1));
        plus.addActionListener(e -> changeQuantity(1));
        addBtn.addActionListener(e -> addOrder());
        stepper.add(UiSupport.muted("Quantity"));
        stepper.add(minus);
        stepper.add(qtyLabel);
        stepper.add(plus);
        stepper.add(addBtn);
        footer.add(stepper, BorderLayout.EAST);
        return footer;
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
            hint.setText(rooms.stream().anyMatch(Room::occupied) ? " " : "No guests are checked in.");
        });
    }

    private void onRoomPicked(Room room) {
        addBtn.setEnabled(room != null);
        if (room == null) {
            clearTable();
        } else {
            loadBill(room);
        }
    }

    private void onItemPicked(MenuItem item) {
        chosenItem = item;
        chosenLabel.setText(item.name() + "  -  " + UiSupport.money(item.price()) + " each");
        chosenLabel.setForeground(UiSupport.TEXT);
    }

    private void changeQuantity(int delta) {
        quantity = Math.max(1, Math.min(HotelService.MAX_QUANTITY, quantity + delta));
        qtyLabel.setText(String.valueOf(quantity));
    }

    private void addOrder() {
        Room room = roomPicker.selected();
        MenuItem item = chosenItem;
        if (room == null) {
            UiSupport.info(this, "Choose a checked-in room first.");
            return;
        }
        if (item == null) {
            UiSupport.info(this, "Tap an item in the menu first.");
            return;
        }
        int qty = quantity;
        UiSupport.background(this, () -> service.addFood(room.number(), item.id(), qty), line -> {
            hint.setText("Added " + qty + " x " + item.name() + " to room " + room.number() + ".");
            loadBill(room);
        });
    }

    private void loadBill(Room room) {
        UiSupport.background(this, () -> service.previewBill(room.number()), bill -> {
            Room current = roomPicker.selected();
            if (current != null && current.number() == room.number()) {
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
