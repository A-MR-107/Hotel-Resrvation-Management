package com.hotel.ui;

import com.hotel.billing.BillFormatter;
import com.hotel.model.Room;
import com.hotel.service.HotelException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
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

/** Palette (warm espresso background, gold accent), fonts and shared helpers. */
final class UiSupport {

    static final Color ACCENT = new Color(0xE4E4E4);
    static final Color ACCENT_HI = new Color(0xFFFFFF);
    static final Color ACCENT_LO = new Color(0xB8BEC3);
    static final Color ACCENT_PRESS = new Color(0x8E959A);
    static final Color ON_ACCENT = new Color(0x0A0B0C);
    static final Color TEXT = new Color(0xF2F2F2);
    static final Color MUTED = new Color(0x9A9FA4);
    static final Color FREE_BG = new Color(0x1A1D20);
    static final Color FREE_BORDER = new Color(0xA9B4BB);
    static final Color FREE_TEXT = new Color(0xD5DCE0);
    static final Color BUSY_BG = new Color(0x2A1B1B);
    static final Color BUSY_BORDER = new Color(0xE0736A);
    static final Color BUSY_TEXT = new Color(0xF2B3AD);
    static final Color LINE = new Color(255, 255, 255, 46);

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

    static void enterMovesNext(javax.swing.JComponent c) {
        c.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "hotel.next");
        c.getActionMap().put("hotel.next", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                c.transferFocus();
            }
        });
    }

    static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** A frosted-glass card with padding and a BorderLayout. */
    static JPanel card() {
        GlassPanel p = new GlassPanel(22);
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        return p;
    }

    static Border pad(int px) {
        return BorderFactory.createEmptyBorder(px, px, px, px);
    }

    static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        l.setForeground(TEXT);
        return l;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        return l;
    }

    static void row(JPanel form, int row, String label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 6, 16);
        JLabel key = new JLabel(label);
        key.setForeground(MUTED);
        form.add(key, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        form.add(field, c);
    }

    static JLabel sectionHeading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        l.setForeground(ACCENT);
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
