package com.hotel.billing;

import com.hotel.model.Bill;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javax.swing.JOptionPane;

/** Sends a bill to a printer through the system print dialog. */
public final class BillPrinter {

    private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 10);

    private BillPrinter() {
    }

    /** Returns true if the job was sent to the printer. */
    public static boolean print(Component parent, Bill bill) {
        String[] lines = BillFormatter.toText(bill).split("\n");
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Bill - Room " + bill.room().number());
        job.setPrintable(new BillPage(lines));
        if (!job.printDialog()) {
            return false;
        }
        try {
            job.print();
            return true;
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(parent, "Printing failed:\n" + e.getMessage(),
                    "Print error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private record BillPage(String[] lines) implements Printable {
        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setFont(FONT);
            FontMetrics fm = g2.getFontMetrics();
            int lineHeight = fm.getHeight();
            int linesPerPage = Math.max(1, (int) (pf.getImageableHeight() / lineHeight));
            int pages = (lines.length + linesPerPage - 1) / linesPerPage;
            if (pageIndex >= pages) {
                return Printable.NO_SUCH_PAGE;
            }
            g2.translate(pf.getImageableX(), pf.getImageableY());
            int y = fm.getAscent();
            int start = pageIndex * linesPerPage;
            int end = Math.min(lines.length, start + linesPerPage);
            for (int i = start; i < end; i++) {
                g2.drawString(lines[i], 0, y);
                y += lineHeight;
            }
            return Printable.PAGE_EXISTS;
        }
    }
}
