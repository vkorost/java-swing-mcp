package com.swingmcp.demo;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;

public class MenuActionListener implements ActionListener {

    AppLogger log;
    ComponentManager componentManager = new ComponentManager();
    ResourceManager resourceManager = new ResourceManager();

    public void actionPerformed(ActionEvent event) {
        String eventText = event.getActionCommand().toLowerCase();
        try {
            if (eventText.equals("blotter")) {
                log.info("Opening blotter window...");
                componentManager.showBlotter();
            } else if (eventText.equals("portfolios") || eventText.equals("instruments")) {
                log.info("Opening portfolios window...");
                JInternalFrame frame = componentManager.getInstrumentFrame();
                frame.setVisible(true);
                frame.setMaximum(false);
                frame.setIcon(false);
            } else if (eventText.equals("order")) {
                log.info("Opening order window...");
                JInternalFrame frame = componentManager.getOrder();
                frame.setVisible(true);
                frame.setMaximum(false);
                frame.setIcon(false);
            } else if (eventText.equals("about")) {
                ImageIcon icon = null;
                java.net.URL iconUrl = getClass().getResource("/icons/dollars3.png");
                if (iconUrl != null) {
                    Image scaled = new ImageIcon(iconUrl).getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(scaled);
                }
                JOptionPane.showMessageDialog(
                    resourceManager.getDesktopPane(),
                    "Java Swing MCP Server demo application - 2026",
                    "Equity Trading",
                    JOptionPane.INFORMATION_MESSAGE,
                    icon);
            } else if (eventText.equals("save layout")) {
                log.info("Saving layout...");
                LayoutManager.saveLayout();
            } else if (eventText.equals("close")) {
                System.exit(0);
            }
        } catch (Exception e) {
            log.error("Exception ", e);
        }
    }
}
