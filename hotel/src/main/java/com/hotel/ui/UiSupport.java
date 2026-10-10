package com.hotel.ui;

import com.hotel.billing.BillFormatter;
import com.hotel.model.Room;
import com.hotel.service.HotelException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.Border;

/** Colours, fonts, component helpers and a background-task runner shared by all screens. */
final class UiSupport {

    static final Color SIDEBAR = new Color(0x1F2A44);
    static final Color ACCENT = new Color(0x2E7D9A);
    static final Color BG = new Color(0xF2F4F7);
    static final Color FREE_BG = new Color(0xE3F4E6);
    static final Color FREE_BORDER = new Color(0x2E7D32);
    static final Color BUSY_BG = new Color(0xFCE4E2);
    static final Color BUSY_BORDER = new Color(0xC62828);
    static final Color MUTED = new Color(0x6B7280);
    static final Color LINE = new Color(0xD8DDE5);

    private UiSupport() {
    }

    interface Work<T> {
        T run() throws Exception;
    }

    /** Runs slow database work off the Swing thread, then calls onSuccess on the Swing thread. */
    static <T> void background(Component owner, Work<T> work, Consumer<T> onSuccess) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.run();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError(owner, e.getCause());
                }
            }
        }.execute();
    }

    static void showError(Component owner, Throwable t) {
        if (t instanceof HotelException) {
            JOptionPane.showMessageDialog(owner, t.getMessage(), "Cannot complete", JOptionPane.WARNING_MESSAGE);
        } else {
            t.printStackTrace();
            JOptionPane.showMessageDialog(owner,
                    "Something went wrong with the database:\n" + t.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static String money(BigDecimal amount) {
        return BillFormatter.money(amount);
    }

    /** Refills a room picker, keeping the wanted room (or the current one) selected if still listed. */
    static void fillRooms(JComboBox<Room> box, List<Room> rooms, Predicate<Room> filter, Integer wanted) {
        Room previous = (Room) box.getSelectedItem();
        Integer keep = wanted != null ? wanted : (previous != null ? previous.number() : null);
        box.removeAllItems();
        for (Room r : rooms) {
            if (filter.test(r)) {
                box.addItem(r);
            }
        }
        if (keep != null) {
            for (int i = 0; i < box.getItemCount(); i++) {
                if (box.getItemAt(i).number() == keep) {
                    box.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    static JPanel card() {
        JPanel p = new JPanel(new java.awt.BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        return p;
    }

    static Border pad(int px) {
        return BorderFactory.createEmptyBorder(px, px, px, px);
    }

    static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        return l;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        return l;
    }

    /** One label/field row in a GridBagLayout form. */
    static void row(JPanel form, int row, String label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 6, 16);
        form.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        form.add(field, c);
    }

    static JLabel sectionHeading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 14f));
        return l;
    }

    static void info(Component owner, String message) {
        JOptionPane.showMessageDialog(owner, message, "Hotel", JOptionPane.INFORMATION_MESSAGE);
    }

    static JPanel borderPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        return p;
    }
}
