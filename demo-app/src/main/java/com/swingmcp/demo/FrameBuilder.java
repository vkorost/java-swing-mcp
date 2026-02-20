package com.swingmcp.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

public class FrameBuilder {

    MenuManager menuManager = new MenuManager();
    ResourceManager resourceManager = new ResourceManager();
    AppLogger log;

    public FrameBuilder() {}

    static void applyFrameIcon(JInternalFrame frame, String iconResource, Color fallbackColor) {
        java.net.URL iconUrl = FrameBuilder.class.getResource(iconResource);
        if (iconUrl != null) {
            Image scaled = new ImageIcon(iconUrl).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            frame.setFrameIcon(new ImageIcon(scaled));
        } else {
            frame.setFrameIcon(createPlaceholderIcon(fallbackColor));
        }
    }

    private static ImageIcon createPlaceholderIcon(Color color) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillRoundRect(1, 1, 14, 14, 4, 4);
        g.dispose();
        return new ImageIcon(img);
    }

    public JFrame buildExternalFrame() {
        JPanel topPanel;
        JDesktopPane desktopPane;
        JFrame frame = new JFrame("Equity Trading");
        frame.setName("mainFrame");
        frame.setSize(1400, 900);

        // Set app window icon
        java.net.URL dollarUrl = getClass().getResource("/icons/moneybag.png");
        if (dollarUrl != null) {
            frame.setIconImage(new ImageIcon(dollarUrl).getImage());
        }
        topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        frame.getContentPane().add(topPanel);

        frame.setJMenuBar(menuManager.getExternalFrameMenu());

        desktopPane = new JDesktopPane();
        desktopPane.setName("desktopPane");
        topPanel.add(desktopPane, BorderLayout.CENTER);

        StatusBarPanel statusBar = resourceManager.getStatusBar();
        statusBar.setName("statusBar");
        topPanel.add(statusBar, BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        resourceManager.setDesktopPane(desktopPane);
        resourceManager.setMainFrame(frame);
        frame.setLocationRelativeTo(null);
        frame.toFront();
        return frame;
    }

    public JInternalFrame buildInternalFrame(AbstractPanel topPanel) {
        log.info("Building internal frame...");
        JInternalFrame frame = new JInternalFrame();
        frame.setTitle(topPanel.getTitle());
        frame.getContentPane().add(topPanel, BorderLayout.CENTER);
        frame.setSize(topPanel.getW(), topPanel.getH());
        frame.setClosable(true);
        frame.setMaximizable(true);
        frame.setIconifiable(true);
        frame.setResizable(true);
        frame.pack();
        return frame;
    }
}
