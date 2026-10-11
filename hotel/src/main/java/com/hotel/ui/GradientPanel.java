package com.hotel.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import javax.swing.JPanel;

/** App background: a dark espresso gradient at a 223deg angle (same geometry as CSS). */
final class GradientPanel extends JPanel {

    GradientPanel() {
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        double rad = Math.toRadians(223);
        double dx = Math.sin(rad);
        double dy = -Math.cos(rad);
        double half = (Math.abs(w * dx) + Math.abs(h * dy)) / 2.0;
        double cx = w / 2.0;
        double cy = h / 2.0;
        Point2D start = new Point2D.Double(cx - dx * half, cy - dy * half);
        Point2D end = new Point2D.Double(cx + dx * half, cy + dy * half);
        g2.setPaint(new LinearGradientPaint(start, end, new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(0x1E1E1E), new Color(0x000000), new Color(0x141617)}));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }
}
