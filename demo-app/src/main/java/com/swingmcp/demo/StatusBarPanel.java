package com.swingmcp.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

public class StatusBarPanel extends JPanel {

    private final JLabel statusLabel;
    private final JLabel timeLabel;
    private final JLabel memoryLabel;
    private final Timer timer;

    public StatusBarPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));

        // Left: status message
        statusLabel = new JLabel(" ");
        statusLabel.setName("statusLabel");
        add(statusLabel, BorderLayout.CENTER);

        // Right: system info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

        String userName = System.getProperty("user.name");
        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostName = "unknown";
        }
        String timezone = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT);

        infoPanel.add(new JLabel(userName + "  |  "));
        infoPanel.add(new JLabel(hostName + "  |  "));
        infoPanel.add(new JLabel(timezone + "  |  "));

        timeLabel = new JLabel();
        infoPanel.add(timeLabel);
        infoPanel.add(new JLabel("  |  "));

        memoryLabel = new JLabel();
        infoPanel.add(memoryLabel);

        add(infoPanel, BorderLayout.EAST);

        // Update time and memory immediately
        updateTimeAndMemory();

        // Timer to update every second
        timer = new Timer(1000, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                updateTimeAndMemory();
            }
        });
        timer.start();
    }

    private void updateTimeAndMemory() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        timeLabel.setText(sdf.format(new Date()));

        Runtime rt = Runtime.getRuntime();
        long freeMB = rt.freeMemory() / (1024 * 1024);
        long totalMB = rt.totalMemory() / (1024 * 1024);
        memoryLabel.setText(freeMB + " / " + totalMB + " MB");
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    public void setStatusColor(Color color) {
        statusLabel.setForeground(color);
    }
}
