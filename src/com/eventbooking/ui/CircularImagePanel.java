package com.eventbooking.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * A custom JPanel that displays an image within a circular mask.
 */
public class CircularImagePanel extends JPanel {

    private BufferedImage masterImage;
    private int diameter;

    public CircularImagePanel(int diameter) {
        this.diameter = diameter;
        Dimension size = new Dimension(diameter, diameter);
        this.setPreferredSize(size);
        this.setMinimumSize(size);
        this.setMaximumSize(size);
        this.setOpaque(false);
    }

    /**
     * Loads an image from the specified file path.
     * If the path is null, empty, or the image fails to load, it shows a placeholder.
     * @param imagePath The path to the image file.
     */
    public void loadImage(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            masterImage = null;
            repaint();
            return;
        }

        try {
            masterImage = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            System.err.println("Error loading profile image: " + e.getMessage());
            masterImage = null;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (masterImage != null) {
            // Create a circular clipping region
            Shape clip = new Ellipse2D.Float(0, 0, diameter, diameter);
            g2d.setClip(clip);

            // Scale and draw the image to fill the circle
            g2d.drawImage(masterImage, 0, 0, diameter, diameter, null);
            
            // Draw a border around the circle
            g2d.setColor(new Color(52, 152, 219));
            g2d.setStroke(new BasicStroke(3));
            g2d.draw(clip);

        } else {
            // If no image is loaded, draw a placeholder
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillOval(0, 0, diameter, diameter);

            g2d.setColor(Color.DARK_GRAY);
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            String text = "No Image";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (diameter - fm.stringWidth(text)) / 2;
            int y = (fm.getAscent() + (diameter - (fm.getAscent() + fm.getDescent())) / 2);
            g2d.drawString(text, x, y);

            g2d.setColor(new Color(52, 152, 219));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(0, 0, diameter, diameter);
        }
        
        g2d.dispose();
    }
}