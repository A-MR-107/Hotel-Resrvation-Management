package com.hotel.ui;

import com.hotel.service.HotelService;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Main window: sidebar navigation on the left, one screen at a time in the middle. */
public final class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel sidebar = new JPanel();
    private final Map<String, Screen> screens = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private String current;

    public MainFrame(HotelService service, String dbInfo) {
        super("Hotel Management System");

        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UiSupport.SIDEBAR);
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 14, 22, 14));
        JLabel brand = new JLabel("Hotel Manager");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(24));

        addScreen("dashboard", "Dashboard", new DashboardPanel(service, this::open));
        addScreen("checkin", "Check-in", new CheckinPanel(service));
        addScreen("food", "Food orders", new FoodPanel(service));
        addScreen("checkout", "Checkout & bill", new CheckoutPanel(service));
        sidebar.add(Box.createVerticalGlue());

        JLabel status = new JLabel("Database: " + dbInfo);
        status.setForeground(UiSupport.MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(sidebar, BorderLayout.WEST);
        getContentPane().add(content, BorderLayout.CENTER);
        getContentPane().add(status, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(1240, 780);
        setLocationRelativeTo(null);
        show("dashboard");
    }

    private void addScreen(String key, String label, Screen screen) {
        screens.put(key, screen);
        content.add((Component) screen, key);

        JButton button = new JButton(label);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.addActionListener(e -> show(key));
        navButtons.put(key, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(6));
    }

    /** Opens a screen with a room preselected (used by the dashboard quick actions). */
    void open(String key, int roomNumber) {
        screens.get(key).focusRoom(roomNumber);
        show(key);
    }

    void show(String key) {
        current = key;
        cards.show(content, key);
        screens.get(key).refresh();
        navButtons.forEach((k, b) -> {
            boolean active = k.equals(current);
            b.setBackground(active ? UiSupport.ACCENT : UiSupport.SIDEBAR);
            b.setForeground(Color.WHITE);
            b.setOpaque(true);
            b.setBorderPainted(false);
        });
    }
}
