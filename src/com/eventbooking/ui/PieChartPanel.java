package com.eventbooking.ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import org.bson.Document;

public class PieChartPanel extends JPanel {
    private List<Document> data;
    private List<Color> sliceColors;

    public PieChartPanel(List<Document> data) {
        this.data = data;
        this.sliceColors = generateColors(data.size());
        setPreferredSize(new Dimension(600, 450));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private List<Color> generateColors(int count) {
        List<Color> colors = new ArrayList<>();
        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < count; i++) {
            colors.add(new Color(rand.nextInt(210), rand.nextInt(210), rand.nextInt(210)));
        }
        return colors;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data == null || data.isEmpty()) {
            g.drawString("No data available to display chart.", 20, 20);
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double totalRevenue = data.stream().mapToDouble(d -> d.getDouble("revenue")).sum();
        if (totalRevenue == 0) {
            g.drawString("Total revenue is zero, cannot draw chart.", 20, 20);
            return;
        }
        
        int chartX = 50;
        int chartY = 50;
        int chartSize = 350;
        int legendX = chartX + chartSize + 30;
        int legendY = chartY;

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));

        double currentAngle = 0.0;
        for (int i = 0; i < data.size(); i++) {
            Document doc = data.get(i);
            double revenue = doc.getDouble("revenue");
            double arcAngle = (revenue / totalRevenue) * 360.0;

            // Draw slice
            g2d.setColor(sliceColors.get(i));
            g2d.fillArc(chartX, chartY, chartSize, chartSize, (int) currentAngle, (int) Math.ceil(arcAngle));
            
            // Draw legend
            g2d.fillRect(legendX, legendY + (i * 25), 15, 15);
            g2d.setColor(Color.BLACK);
            String category = doc.getString("category");
            double percentage = (revenue / totalRevenue) * 100;
            g2d.drawString(String.format("%s - $%.2f (%.1f%%)", category, revenue, percentage), legendX + 25, legendY + (i * 25) + 13);
            
            currentAngle += arcAngle;
        }
    }
}