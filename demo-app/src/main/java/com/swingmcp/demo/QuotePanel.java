package com.swingmcp.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Random;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class QuotePanel extends AbstractPanel implements MouseListener {

    AppLogger log;
    JTable table;
    ComponentManager componentManager = new ComponentManager();
    ResourceManager resourceManager = new ResourceManager();

    String[] columnNames = {"Symbol", "Last", "Change", "Change%", "Bid", "Ask", "Volume", "High", "Low", "Open", "Market Cap"};
    String[][] dataValues;
    private Timer marketDataTimer;
    private double[] basePrices;
    private double[] currentLasts;
    private double[] bidAskSpreads;

    private static final HashMap activePanels = new HashMap();
    private static final HashMap SECTOR_DATA = new HashMap();
    static {
        SECTOR_DATA.put("Technology", new String[][] {
            {"AAPL",  "242.50", "+1.85", "+0.77%", "242.48", "242.52", "48,200,000", "243.80", "240.15", "241.00", "3.72T"},
            {"MSFT",  "412.30", "-2.10", "-0.51%", "412.28", "412.35", "22,100,000", "415.50", "410.80", "414.00", "3.06T"},
            {"GOOGL", "178.90", "+3.45", "+1.97%", "178.88", "178.95", "28,500,000", "179.50", "175.20", "176.00", "2.19T"},
            {"NVDA",  "135.60", "+4.20", "+3.20%", "135.58", "135.65", "312,000,000", "136.80", "131.00", "132.50", "3.32T"},
            {"META",  "612.40", "+8.30", "+1.37%", "612.35", "612.50", "18,900,000", "615.00", "604.20", "605.00", "1.56T"},
            {"AMZN",  "228.15", "+1.20", "+0.53%", "228.10", "228.20", "35,600,000", "229.50", "226.80", "227.00", "2.38T"},
            {"TSLA",  "352.80", "-5.60", "-1.56%", "352.75", "352.90", "89,400,000", "358.00", "348.50", "357.00", "1.13T"},
            {"AMD",   "118.25", "+2.15", "+1.85%", "118.20", "118.30", "42,800,000", "119.50", "116.00", "116.50", "191B"}
        });
        SECTOR_DATA.put("Financials", new String[][] {
            {"JPM",  "258.40", "+1.90", "+0.74%", "258.35", "258.45", "8,200,000",  "259.80", "256.50", "257.00", "744B"},
            {"BAC",   "46.85", "+0.35", "+0.75%",  "46.83",  "46.88", "32,100,000",  "47.20",  "46.50",  "46.60", "366B"},
            {"GS",   "612.50", "+5.80", "+0.96%", "612.45", "612.60",  "2,100,000", "615.00", "607.00", "608.00", "208B"},
            {"MS",   "128.70", "+1.15", "+0.90%", "128.65", "128.75",  "5,800,000", "129.50", "127.50", "127.80", "206B"},
            {"WFC",   "73.20", "+0.45", "+0.62%",  "73.18",  "73.25", "14,500,000",  "73.80",  "72.80",  "72.90", "264B"},
            {"C",     "78.50", "-0.30", "-0.38%",  "78.48",  "78.55", "11,200,000",  "79.00",  "78.00",  "78.80", "148B"}
        });
        SECTOR_DATA.put("Healthcare", new String[][] {
            {"JNJ",  "152.30", "-0.80", "-0.52%", "152.25", "152.35",  "6,800,000", "153.50", "151.80", "153.00", "367B"},
            {"UNH",  "528.40", "+3.20", "+0.61%", "528.35", "528.50",  "3,200,000", "530.00", "525.00", "526.00", "487B"},
            {"PFE",   "26.15", "+0.25", "+0.97%",  "26.12",  "26.18", "28,900,000",  "26.50",  "25.90",  "25.95", "148B"},
            {"ABBV", "188.60", "+1.40", "+0.75%", "188.55", "188.65",  "5,400,000", "189.80", "187.20", "187.50", "334B"},
            {"MRK",  "100.80", "-0.55", "-0.54%", "100.75", "100.85",  "9,100,000", "101.50", "100.20", "101.20", "255B"}
        });
        SECTOR_DATA.put("Energy", new String[][] {
            {"XOM",  "108.50", "+0.90", "+0.84%", "108.45", "108.55", "12,800,000", "109.20", "107.80", "108.00", "459B"},
            {"CVX",  "155.20", "+1.10", "+0.71%", "155.15", "155.25",  "6,500,000", "156.00", "154.20", "154.50", "284B"},
            {"COP",  "109.80", "+0.65", "+0.60%", "109.75", "109.85",  "4,800,000", "110.50", "109.00", "109.20", "132B"},
            {"SLB",   "42.30", "+0.40", "+0.95%",  "42.28",  "42.35",  "8,900,000",  "42.80",  "41.90",  "42.00",  "60B"}
        });
        SECTOR_DATA.put("ETFs", new String[][] {
            {"SPY", "604.80", "+2.40", "+0.40%", "604.75", "604.85", "52,000,000", "606.00", "602.50", "603.00", "-"},
            {"QQQ", "525.30", "+4.10", "+0.79%", "525.25", "525.40", "38,000,000", "527.00", "521.00", "522.00", "-"},
            {"IWM", "226.50", "+1.80", "+0.80%", "226.45", "226.55", "22,000,000", "227.50", "224.80", "225.00", "-"},
            {"DIA", "440.20", "+1.50", "+0.34%", "440.15", "440.25",  "3,200,000", "441.50", "438.80", "439.00", "-"}
        });
    }

    public static String[] findQuoteData(String sectorName, String symbol) {
        // First check active ticking panels for live data
        QuotePanel activePanel = (QuotePanel) activePanels.get(sectorName);
        if (activePanel != null && activePanel.dataValues != null) {
            for (int i = 0; i < activePanel.dataValues.length; i++) {
                if (activePanel.dataValues[i][0].equals(symbol)) {
                    return activePanel.dataValues[i];
                }
            }
        }
        // Fall back to static data
        String[][] sectorRows = (String[][]) SECTOR_DATA.get(sectorName);
        if (sectorRows == null) return null;
        for (int i = 0; i < sectorRows.length; i++) {
            if (sectorRows[i][0].equals(symbol)) {
                return sectorRows[i];
            }
        }
        return null;
    }

    private static double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s.replace(",", "").replace("+", "").replace("%", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void populatePanel(String sectorName) {
        this.setLayout(new BorderLayout());

        // Deep copy so ticking doesn't mutate static SECTOR_DATA
        String[][] source = (String[][]) SECTOR_DATA.get(sectorName);
        if (source == null) {
            dataValues = new String[0][0];
        } else {
            dataValues = new String[source.length][];
            for (int i = 0; i < source.length; i++) {
                dataValues[i] = new String[source[i].length];
                System.arraycopy(source[i], 0, dataValues[i], 0, source[i].length);
            }
        }

        table = new JTable();
        table.setName("quoteTable_" + sectorName);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        QuoteDataModel dataModel = new QuoteDataModel();
        table.setModel(dataModel);
        table.addMouseListener(this);

        // Set preferred column widths
        if (table.getColumnCount() >= 11) {
            table.getColumnModel().getColumn(0).setPreferredWidth(60);   // Symbol
            table.getColumnModel().getColumn(1).setPreferredWidth(70);   // Last
            table.getColumnModel().getColumn(2).setPreferredWidth(65);   // Change
            table.getColumnModel().getColumn(3).setPreferredWidth(70);   // Change%
            table.getColumnModel().getColumn(4).setPreferredWidth(70);   // Bid
            table.getColumnModel().getColumn(5).setPreferredWidth(70);   // Ask
            table.getColumnModel().getColumn(6).setPreferredWidth(90);   // Volume
            table.getColumnModel().getColumn(7).setPreferredWidth(65);   // High
            table.getColumnModel().getColumn(8).setPreferredWidth(65);   // Low
            table.getColumnModel().getColumn(9).setPreferredWidth(65);   // Open
            table.getColumnModel().getColumn(10).setPreferredWidth(80);  // Market Cap
        }

        // Right-align all numeric columns (1-10, skip Symbol at 0)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
        for (int col = 1; col < table.getColumnCount(); col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(rightRenderer);
        }

        // Apply color+right-aligned renderer to Change and Change% columns
        QuoteCellRenderer changeRenderer = new QuoteCellRenderer();
        if (table.getColumnCount() > 2) {
            table.getColumnModel().getColumn(2).setCellRenderer(changeRenderer);
        }
        if (table.getColumnCount() > 3) {
            table.getColumnModel().getColumn(3).setCellRenderer(changeRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setName("quoteScrollPane_" + sectorName);
        this.add(scrollPane, BorderLayout.CENTER);

        // Register this panel for live data sharing
        activePanels.put(sectorName, this);

        // Start ticking market data
        startMarketDataTimer();
    }

    private void startMarketDataTimer() {
        if (dataValues == null || dataValues.length == 0) return;

        int rows = dataValues.length;
        basePrices = new double[rows];
        currentLasts = new double[rows];
        bidAskSpreads = new double[rows];

        for (int i = 0; i < rows; i++) {
            double last = parseDoubleSafe(dataValues[i][1]);
            double change = parseDoubleSafe(dataValues[i][2]);
            double bid = parseDoubleSafe(dataValues[i][4]);
            double ask = parseDoubleSafe(dataValues[i][5]);
            basePrices[i] = last - change;
            currentLasts[i] = last;
            bidAskSpreads[i] = ask - bid;
        }

        final Random random = new Random();
        marketDataTimer = new Timer(333, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < dataValues.length; i++) {
                    double delta = (random.nextDouble() * 0.06) - 0.03;
                    currentLasts[i] += delta;
                    double change = currentLasts[i] - basePrices[i];
                    double changePct = basePrices[i] != 0 ? (change / basePrices[i]) * 100 : 0;
                    double halfSpread = bidAskSpreads[i] / 2;

                    dataValues[i][1] = String.format("%.2f", currentLasts[i]);
                    dataValues[i][2] = String.format("%+.2f", change);
                    dataValues[i][3] = String.format("%+.2f%%", changePct);
                    dataValues[i][4] = String.format("%.2f", currentLasts[i] - halfSpread);
                    dataValues[i][5] = String.format("%.2f", currentLasts[i] + halfSpread);
                }
                int[] sel = table.getSelectedRows();
                ((AbstractTableModel) table.getModel()).fireTableDataChanged();
                for (int s : sel) {
                    if (s < table.getRowCount()) table.addRowSelectionInterval(s, s);
                }
            }
        });
        marketDataTimer.start();
    }

    public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int iSelectedRow = table.rowAtPoint(new Point(x, y));
        if (iSelectedRow >= 0) {
            log.info("Selected quote: " + dataValues[iSelectedRow][0]);
            if (e.getClickCount() >= 2) {
                log.info("Initiating order entry...");
                Order order = componentManager.getOrder();
                order.setBenchmarkData(dataValues[iSelectedRow]);
                order.fillControls();
                order.setVisible(true);
            }
        }
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}

    class QuoteDataModel extends AbstractTableModel {
        public boolean isCellEditable(int row, int column) { return false; }
        public int getColumnCount() { return columnNames.length; }
        public int getRowCount() { return dataValues.length; }
        public Object getValueAt(int row, int col) { return dataValues[row][col]; }
        public String getColumnName(int col) { return columnNames[col]; }
    }

    class QuoteCellRenderer extends DefaultTableCellRenderer {
        QuoteCellRenderer() {
            setHorizontalAlignment(RIGHT);
        }
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String colName = table.getColumnName(column);
                if ("Change".equals(colName) || "Change%".equals(colName)) {
                    String val = value != null ? value.toString() : "";
                    if (val.startsWith("+")) {
                        c.setForeground(new Color(0, 200, 0));
                    } else if (val.startsWith("-")) {
                        c.setForeground(new Color(255, 80, 80));
                    } else {
                        c.setForeground(table.getForeground());
                    }
                } else {
                    c.setForeground(table.getForeground());
                }
            }
            return c;
        }
    }
}
