package com.hotel.ui;

import com.hotel.billing.BillFormatter;
import com.hotel.billing.BillPrinter;
import com.hotel.model.Bill;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** A separate window that shows one bill, with Print and Save buttons. */
public final class BillWindow extends JFrame {

    private final Bill bill;

    public BillWindow(Component parent, Bill bill) {
        super("Bill - Room " + bill.room().number());
        this.bill = bill;

        JTextArea text = new JTextArea(BillFormatter.toText(bill));
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        text.setMargin(new Insets(18, 22, 18, 22));
        text.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(BorderFactory.createLineBorder(UiSupport.LINE));

        PillButton print = new PillButton("Print...");
        print.addActionListener(e -> BillPrinter.print(this, this.bill));
        PillButton save = new PillButton("Save as text...", PillButton.Style.SECONDARY);
        save.addActionListener(e -> saveAsText());
        PillButton close = new PillButton("Close", PillButton.Style.SECONDARY);
        close.addActionListener(e -> dispose());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setOpaque(false);
        bar.add(save);
        bar.add(print);
        bar.add(close);

        JPanel root = new GradientPanel();
        root.setLayout(new BorderLayout(0, 8));
        root.setBorder(UiSupport.pad(16));
        root.add(scroll, BorderLayout.CENTER);
        root.add(bar, BorderLayout.SOUTH);
        setContentPane(root);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(580, 740);
        setLocationRelativeTo(parent);
    }

    private void saveAsText() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save bill");
        chooser.setSelectedFile(new File("bill-room-" + bill.room().number() + ".txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        try {
            Files.writeString(target, BillFormatter.toText(bill), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Saved to " + target, "Bill saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save: " + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
