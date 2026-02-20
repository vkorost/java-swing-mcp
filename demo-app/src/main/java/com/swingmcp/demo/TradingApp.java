package com.swingmcp.demo;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.swingmcp.server.SwingMcpServer;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

public class TradingApp {

    static AppLogger log;

    public TradingApp() {
        // Build MDI parent frame
        JFrame mainFrame = new FrameBuilder().buildExternalFrame();
        LayoutManager.restoreMainFrame();
        mainFrame.setVisible(true);

        // Auto-show instrument panel at startup
        ComponentManager componentManager = new ComponentManager();
        try {
            JInternalFrame instrumentFrame = componentManager.getInstrumentFrame();
            if (!LayoutManager.restoreInternalFrame(instrumentFrame)) {
                instrumentFrame.setVisible(true);
                instrumentFrame.setMaximum(false);
                instrumentFrame.setIcon(false);
            }
        } catch (Exception e) {
            log.error("Failed to show instrument panel", e);
        }

        // Restore tree expand/collapse state
        LayoutManager.restoreTreeState();

        // Auto-show blotter at startup, deferred so desktop has final dimensions
        final ComponentManager cm = componentManager;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!LayoutManager.restoreInternalFrame(cm.getBlotterFrame())) {
                    cm.showBlotter();
                }

                // Restore saved quote windows
                List quoteSectors = LayoutManager.getSavedQuoteSectors();
                for (int i = 0; i < quoteSectors.size(); i++) {
                    String sector = (String) quoteSectors.get(i);
                    try {
                        JInternalFrame quoteFrame = cm.getQuoteFrame(sector);
                        if (!LayoutManager.restoreInternalFrame(quoteFrame)) {
                            quoteFrame.setVisible(true);
                        }
                    } catch (Exception e) {
                        log.error("Failed to restore quote frame: " + sector, e);
                    }
                }

                // Restore saved order tickets
                List orderSymbols = LayoutManager.getSavedOrderSymbols();
                for (int i = 0; i < orderSymbols.size(); i++) {
                    String symbol = (String) orderSymbols.get(i);
                    try {
                        String sector = LayoutManager.findSectorForSymbol(symbol);
                        if (sector != null) {
                            String[] quoteData = QuotePanel.findQuoteData(sector, symbol);
                            if (quoteData != null) {
                                Order order = cm.getOrder();
                                order.setBenchmarkData(quoteData);
                                order.fillControls();
                                if (!LayoutManager.restoreInternalFrame(order)) {
                                    order.setVisible(true);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to restore order ticket: " + symbol, e);
                    }
                }
            }
        });

        // Start MCP server
        SwingMcpServer.start(9222);
        log.info("SwingMCP Server running on http://localhost:9222");
    }

    public static void main(String[] args) {
        // Apply FlatLaf Darcula theme before any UI construction
        FlatDarculaLaf.setup();

        log.info("Starting Equity Trading App...");
        new TradingApp();
    }
}
