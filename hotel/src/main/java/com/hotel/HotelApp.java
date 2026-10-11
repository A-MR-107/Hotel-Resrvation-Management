package com.hotel;

import com.hotel.db.Database;
import com.hotel.service.HotelService;
import com.hotel.ui.MainFrame;
import com.hotel.ui.Theme;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class HotelApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                Database db = Database.load();
                db.initialize();
                HotelService service = new HotelService(db);
                new MainFrame(service, db.describe()).setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Could not start the application.\n\n" + e.getMessage()
                                + "\n\nCheck db.properties and make sure MySQL is running.",
                        "Startup error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
