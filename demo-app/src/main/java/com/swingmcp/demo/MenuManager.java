package com.swingmcp.demo;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MenuManager {

    public MenuManager() {}

    public JMenuBar getExternalFrameMenu() {
        JMenuBar menuBar = new JMenuBar();

        // FILE
        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic('F');
        JMenuItem menuFileSaveLayout = createMenuItem(menuFile, "Save Layout", 'S', "Save window layout");
        menuFile.addSeparator();
        JMenuItem menuFileClose = createMenuItem(menuFile, "Close", 'C', "Exit application");
        menuBar.add(menuFile);

        // VIEW
        JMenu menuView = createMenu("View", 'V');
        JMenuItem menuViewPortfolios = createMenuItem(menuView, "Portfolios", 'P', "Portfolios window");
        JMenuItem menuViewBlotter = createMenuItem(menuView, "Blotter", 'B', "View blotter window");
        menuBar.add(menuView);

        // HELP
        JMenu menuHelp = createMenu("Help", 'H');
        JMenuItem menuHelpAbout = createMenuItem(menuHelp, "About", 'A', "");
        menuBar.add(menuHelp);

        return menuBar;
    }

    public JMenu createMenu(String sText, int acceleratorKey) {
        JMenu menuItem = new JMenu();
        menuItem.setText(sText);
        if (acceleratorKey > 0)
            menuItem.setMnemonic(acceleratorKey);
        return menuItem;
    }

    public JMenuItem createMenuItem(JMenu menu, String sText, int acceleratorKey, String sToolTip) {
        JMenuItem menuItem = new JMenuItem();
        menuItem.setText(sText);
        if (acceleratorKey > 0)
            menuItem.setMnemonic(acceleratorKey);
        if (sToolTip != null)
            menuItem.setToolTipText(sToolTip);
        menuItem.addActionListener(new MenuActionListener());
        menu.add(menuItem);
        return menuItem;
    }
}
