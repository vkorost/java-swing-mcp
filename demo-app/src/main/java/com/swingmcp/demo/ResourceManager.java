package com.swingmcp.demo;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;

public class ResourceManager {

    AppLogger log;
    private static JFrame mainFrame = null;
    private static JDesktopPane desktopPane = null;
    private static StatusBarPanel statusBarPanel = new StatusBarPanel();
    private static long orderId = 1000;

    public ResourceManager() {}

    public void setMainFrame(JFrame frame) {
        mainFrame = frame;
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public void setDesktopPane(JDesktopPane pane) {
        desktopPane = pane;
    }

    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    public StatusBarPanel getStatusBar() {
        return statusBarPanel;
    }

    public long getNextOrderId() {
        return orderId++;
    }
}
