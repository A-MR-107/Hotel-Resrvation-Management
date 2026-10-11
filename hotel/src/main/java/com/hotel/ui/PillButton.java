package com.hotel.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;

/** Custom-painted button with a full, evenly outlined rounded border. */
final class PillButton extends JButton {

    enum Style { PRIMARY, SECONDARY, NAV }

    private final Style style;
    private boolean active;

    PillButton(String text) {
        this(text, Style.PRIMARY);
    }

    PillButton(String text, Style style) {
        super(text);
        this.style = style;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setFont(getFont().deriveFont(Font.BOLD, style == Style.NAV ? 14f : 13f));
        setMargin(new Insets(8, 18, 8, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (!isEnabled()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        }
        boolean hover = isEnabled() && getModel().isRollover();
        boolean pressed = isEnabled() && getModel().isPressed();
        int w = getWidth();
        int h = getHeight();
        // Inset by half a pixel so the 1px outline is drawn fully inside the component.
        double arc = style == Style.NAV ? 14 : Math.min(w, h) - 1;
        RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, arc, arc);

        boolean solid = style == Style.PRIMARY || active;
        Color text;
        if (solid) {
            Color top = pressed ? UiSupport.ACCENT_PRESS : hover ? UiSupport.ACCENT_HI : UiSupport.ACCENT;
            Color bottom = pressed ? UiSupport.ACCENT_PRESS : hover ? UiSupport.ACCENT : UiSupport.ACCENT_LO;
            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
            g2.fill(shape);
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 80), 0, h / 2f,
                    new Color(255, 255, 255, 0)));
            g2.fill(shape);
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(shape);
            text = UiSupport.ON_ACCENT;
        } else {
            int fill = hover ? 42 : style == Style.SECONDARY ? 22 : 0;
            if (fill > 0) {
                g2.setColor(new Color(255, 255, 255, fill));
                g2.fill(shape);
            }
            if (style == Style.SECONDARY || hover) {
                g2.setColor(new Color(255, 255, 255, hover ? 190 : 110));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(shape);
            }
            text = UiSupport.TEXT;
        }

        Font font = getFont();
        FontMetrics fm = g2.getFontMetrics(font);
        g2.setFont(font);
        g2.setColor(text);
        String label = getText();
        int x = style == Style.NAV ? 18 : (w - fm.stringWidth(label)) / 2;
        int y = (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(label, x, y);
        g2.dispose();
    }
}
