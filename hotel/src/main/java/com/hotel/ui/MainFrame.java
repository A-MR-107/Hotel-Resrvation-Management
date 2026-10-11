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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Main window: glass sidebar on the left, one screen at a time on a gradient background. */
public final class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final GlassPanel sidebar = new GlassPanel(24);
    private final Map<String, Screen> screens = new LinkedHashMap<>();
    private final Map<String, PillButton> navButtons = new LinkedHashMap<>();
    private String current;

    public MainFrame(HotelService service, String dbInfo) {
        super("Hotel Management System");

        GradientPanel root = new GradientPanel();
        root.setLayout(new BorderLayout(18, 0));
        root.setBorder(UiSupport.pad(18));
        setContentPane(root);

        content.setOpaque(false);

        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 20, 16));
        sidebar.setPreferredSize(new Dimension(230, 0));

        JLabel brand = new JLabel("Hotel Manager");
        brand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tagline = UiSupport.muted("Front desk");
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(brand);
        sidebar.add(tagline);
        sidebar.add(Box.createVerticalStrut(28));

        addScreen("dashboard", "Dashboard", new DashboardPanel(service, this::open));
        addScreen("checkin", "Check-in", new CheckinPanel(service));
        addScreen("food", "Food orders", new FoodPanel(service));
        addScreen("checkout", "Checkout & bill", new CheckoutPanel(service));

        sidebar.add(Box.createVerticalGlue());
        JLabel status = UiSupport.muted("Database: " + dbInfo);
        status.setFont(status.getFont().deriveFont(12f));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(status);

        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1280, 800);
        setLocationRelativeTo(null);
        show("dashboard");
    }

    private void addScreen(String key, String label, Screen screen) {
        screens.put(key, screen);
        content.add((Component) screen, key);

        PillButton button = new PillButton(label, PillButton.Style.NAV);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setPreferredSize(new Dimension(200, 46));
        button.addActionListener(e -> show(key));
        navButtons.put(key, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(8));
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
        navButtons.forEach((k, b) -> b.setActive(k.equals(current)));
    }
}
