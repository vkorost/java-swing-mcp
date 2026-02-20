package com.swingmcp.demo;

import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

public class ComponentManager {

    AppLogger log;
    PanelBuilder panelBuilder = new PanelBuilder();
    FrameBuilder frameBuilder = new FrameBuilder();
    ResourceManager resourceManager = new ResourceManager();
    private static JInternalFrame blotterFrame = null;
    private static JInternalFrame instrumentFrame = null;
    JOptionPane msgBox = new JOptionPane();
    private static BlotterPanel blotterPanel = null;

    public ComponentManager() {
    }

    public BlotterPanel getBlotterPanel() {
        if (blotterPanel == null) {
            blotterPanel = panelBuilder.buildBlotterPanel();
        }
        return blotterPanel;
    }

    public JInternalFrame getBlotterFrame() {
        if (blotterFrame == null) {
            blotterFrame = frameBuilder.buildInternalFrame(getBlotterPanel());
            blotterFrame.setName("blotterFrame");
            FrameBuilder.applyFrameIcon(blotterFrame, "/icons/blotter.png", new Color(0, 188, 212));
            resourceManager.getDesktopPane().add(blotterFrame, JLayeredPane.PALETTE_LAYER);
            blotterFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        }
        return blotterFrame;
    }

    public JInternalFrame getQuoteFrame(String title) {
        AbstractPanel panel = panelBuilder.buildQuotePanel(title);
        JInternalFrame quoteFrame = frameBuilder.buildInternalFrame(panel);
        quoteFrame.setName("quoteFrame_" + title);
        FrameBuilder.applyFrameIcon(quoteFrame, "/icons/md.png", new Color(66, 133, 244));
        resourceManager.getDesktopPane().add(quoteFrame, JLayeredPane.PALETTE_LAYER);
        quoteFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        // Position right of instrument list, filling available space above blotter
        JDesktopPane desktop = resourceManager.getDesktopPane();
        JInternalFrame instFrame = getInstrumentFrame();
        JInternalFrame blotFrame = getBlotterFrame();

        int x = instFrame.getX() + instFrame.getWidth() + 2;
        int y = instFrame.getY();
        int availableWidth = desktop.getWidth() - x - 2;
        int blotterTop = (blotFrame != null && blotFrame.isVisible()) ? blotFrame.getY() : desktop.getHeight();
        int availableHeight = blotterTop - y - 2;

        quoteFrame.setBounds(x, y, Math.max(availableWidth, 900), Math.max(availableHeight, 300));

        return quoteFrame;
    }

    public JInternalFrame getInstrumentFrame() {
        if (instrumentFrame == null) {
            AbstractPanel panel = panelBuilder.buildInstrumentPanel();
            instrumentFrame = frameBuilder.buildInternalFrame(panel);
            instrumentFrame.setName("instrumentFrame");
            FrameBuilder.applyFrameIcon(instrumentFrame, "/icons/book.png", new Color(76, 175, 80));
            resourceManager.getDesktopPane().add(instrumentFrame, JLayeredPane.PALETTE_LAYER);
            instrumentFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        }
        return instrumentFrame;
    }

    public Order getOrder() {
        Order order = new Order();
        FrameBuilder.applyFrameIcon(order, "/icons/ticket.png", new Color(255, 152, 0));
        resourceManager.getDesktopPane().add(order, JLayeredPane.PALETTE_LAYER);
        order.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        // Center in desktop with random offset
        JDesktopPane desktop = resourceManager.getDesktopPane();
        int x = (desktop.getWidth() - order.getWidth()) / 2 + (int)(Math.random() * 40 - 20);
        int y = (desktop.getHeight() - order.getHeight()) / 2 + (int)(Math.random() * 40 - 20);
        order.setLocation(Math.max(0, x), Math.max(0, y));

        return order;
    }

    public void showErrorMsg(String sMsg, JComponent component) {
        msgBox.showMessageDialog(component, sMsg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfoMsg(String sMsg, JComponent component) {
        msgBox.showMessageDialog(component, sMsg, "Trading App", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showBlotter() {
        try {
            JInternalFrame frame = getBlotterFrame();
            frame.setVisible(true);
            frame.setMaximum(false);
            frame.setIcon(false);

            // Dock at bottom of desktop, full width
            JDesktopPane desktop = resourceManager.getDesktopPane();
            int blotterHeight = 250;
            frame.setBounds(0, desktop.getHeight() - blotterHeight, desktop.getWidth(), blotterHeight);
        } catch (Exception e) {
            log.error("Unable to show blotter !!!", e);
        }
    }
}
