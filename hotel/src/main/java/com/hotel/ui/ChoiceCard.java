package com.hotel.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** One selectable option tile: a title and a subtitle. Gold outline and tint when selected. */
final class ChoiceCard extends JPanel {

    private final JLabel title = new JLabel();
    private final JLabel subtitle = new JLabel();
    private boolean selected;
    private boolean hover;

    ChoiceCard(String titleText, String subtitleText) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(150, 78));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        title.setText(titleText);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setForeground(UiSupport.TEXT);
        subtitle.setText(subtitleText);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitle.setForeground(UiSupport.MUTED);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 4));
        text.setOpaque(false);
        text.add(title);
        text.add(subtitle);
        add(text, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    void setSelected(boolean selected) {
        this.selected = selected;
        subtitle.setForeground(selected ? UiSupport.ACCENT : UiSupport.MUTED);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 2;
        int h = getHeight() - 2;
        RoundRectangle2D shape = new RoundRectangle2D.Double(1, 1, w, h, 20, 20);
        if (selected) {
            g2.setColor(UiSupport.withAlpha(UiSupport.ACCENT, 70));
            g2.fill(shape);
            g2.setColor(UiSupport.ACCENT);
            g2.setStroke(new BasicStroke(2f));
        } else {
            g2.setColor(new Color(255, 255, 255, hover ? 46 : 20));
            g2.fill(shape);
            g2.setColor(new Color(255, 255, 255, hover ? 110 : 50));
            g2.setStroke(new BasicStroke(1f));
        }
        g2.draw(shape);
        g2.dispose();
    }
}
