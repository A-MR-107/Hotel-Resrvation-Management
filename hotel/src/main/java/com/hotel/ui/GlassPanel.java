package com.hotel.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

/** A translucent, rounded "frosted glass" card with a soft highlight along the top edge. */
final class GlassPanel extends JPanel {

    private final int radius;

    GlassPanel(int radius) {
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, w, h, radius, radius);
        g2.setColor(new Color(255, 255, 255, 24));
        g2.fill(shape);
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 40), 0, Math.min(h, 160),
                new Color(255, 255, 255, 0)));
        g2.fill(shape);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(shape);
        g2.dispose();
    }
}
