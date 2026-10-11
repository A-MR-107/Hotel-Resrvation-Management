package com.hotel.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;

/** A rounded selectable tile: a room number, or a menu item with its price underneath. */
final class Chip extends JButton {

    private final String subtitle;
    private final Color base;
    private final Color border;
    private boolean selected;

    Chip(String title, String subtitle, Color base, Color border, int width, int height) {
        super(title);
        this.subtitle = subtitle;
        this.base = base;
        this.border = border;
        setPreferredSize(new Dimension(width, height));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, subtitle == null ? 16 : 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    void setChosen(boolean selected) {
        this.selected = selected;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        boolean hover = isEnabled() && getModel().isRollover();
        RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 18, 18);

        Color text;
        if (selected) {
            g2.setPaint(new GradientPaint(0, 0, UiSupport.ACCENT_HI, 0, h, UiSupport.ACCENT_LO));
            g2.fill(shape);
            g2.setColor(new Color(255, 255, 255, 140));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(shape);
            text = UiSupport.ON_ACCENT;
        } else {
            g2.setColor(hover ? lighten(base, 26) : base);
            g2.fill(shape);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(shape);
            text = UiSupport.TEXT;
        }

        Font font = getFont();
        FontMetrics fm = g2.getFontMetrics(font);
        g2.setFont(font);
        g2.setColor(text);
        if (subtitle == null) {
            int x = (w - fm.stringWidth(getText())) / 2;
            int y = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), x, y);
        } else {
            int y1 = h / 2 - 2;
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, y1);
            Font small = font.deriveFont(Font.PLAIN, 12f);
            g2.setFont(small);
            FontMetrics sm = g2.getFontMetrics(small);
            g2.setColor(selected ? UiSupport.ON_ACCENT : UiSupport.MUTED);
            g2.drawString(subtitle, (w - sm.stringWidth(subtitle)) / 2, y1 + sm.getAscent() + 6);
        }
        g2.dispose();
    }

    private static Color lighten(Color c, int amount) {
        return new Color(Math.min(255, c.getRed() + amount), Math.min(255, c.getGreen() + amount),
                Math.min(255, c.getBlue() + amount));
    }
}
