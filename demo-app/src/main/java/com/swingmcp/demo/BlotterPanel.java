package com.swingmcp.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class BlotterPanel extends AbstractPanel {

    AppLogger log;
    HashMap orderPlacement = new HashMap();
    JTable table;

    static final int COL_TIME = 0;
    static final int COL_STATUS = 1;
    static final int COL_SIDE = 2;
    static final int COL_QTY = 3;
    static final int COL_SYMBOL = 4;
    static final int COL_ORDER_TYPE = 5;
    static final int COL_LIMIT_PRICE = 6;
    static final int COL_TIF = 7;
    static final int COL_ROUTE = 8;
    static final int COL_ACCOUNT = 9;
    static final int COL_VALUE = 10;

    //                          0       1         2       3      4         5             6              7      8       9          10
    String[] columnNames = {"Time", "Status", "Side", "Qty", "Symbol", "Order Type", "Limit Price", "TIF", "Route", "Account", "Value"};
    String[][] dataValues = {
        {"10:15:32", "Executed", "BUY",  "100", "AAPL", "MARKET", "",       "DAY", "SMART", "MAIN TRADING",     "24250.00"},
        {"10:22:45", "Executed", "SELL", "200", "MSFT", "LIMIT",  "412.30", "GTC", "NYSE",  "HEDGE FUND ALPHA", "82460.00"}
    };

    public void populatePanel() {
        this.setLayout(new BorderLayout());

        table = new JTable();
        table.setName("blotterTable");
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);

        BlotterDataModel dataModel = new BlotterDataModel();
        table.setModel(dataModel);

        applyRenderers();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setName("blotterScrollPane");
        this.add(scrollPane, BorderLayout.CENTER);
    }

    private void applyRenderers() {
        // Default renderer for all columns
        BlotterCellRenderer renderer = new BlotterCellRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Right-aligned renderer for numeric columns: Qty, Limit Price, Value
        BlotterCellRenderer rightRenderer = new BlotterCellRenderer();
        rightRenderer.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
        if (table.getColumnCount() > COL_QTY) {
            table.getColumnModel().getColumn(COL_QTY).setCellRenderer(rightRenderer);
        }
        if (table.getColumnCount() > COL_LIMIT_PRICE) {
            table.getColumnModel().getColumn(COL_LIMIT_PRICE).setCellRenderer(rightRenderer);
        }
        if (table.getColumnCount() > COL_VALUE) {
            table.getColumnModel().getColumn(COL_VALUE).setCellRenderer(rightRenderer);
        }

        // Set preferred column widths
        if (table.getColumnCount() >= columnNames.length) {
            table.getColumnModel().getColumn(COL_TIME).setPreferredWidth(70);
            table.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(65);
            table.getColumnModel().getColumn(COL_SIDE).setPreferredWidth(50);
            table.getColumnModel().getColumn(COL_QTY).setPreferredWidth(55);
            table.getColumnModel().getColumn(COL_SYMBOL).setPreferredWidth(55);
            table.getColumnModel().getColumn(COL_ORDER_TYPE).setPreferredWidth(80);
            table.getColumnModel().getColumn(COL_LIMIT_PRICE).setPreferredWidth(75);
            table.getColumnModel().getColumn(COL_TIF).setPreferredWidth(40);
            table.getColumnModel().getColumn(COL_ROUTE).setPreferredWidth(55);
            table.getColumnModel().getColumn(COL_ACCOUNT).setPreferredWidth(130);
            table.getColumnModel().getColumn(COL_VALUE).setPreferredWidth(85);
        }
    }

    class BlotterDataModel extends AbstractTableModel {
        public boolean isCellEditable(int row, int column) { return false; }
        public int getColumnCount() { return columnNames.length; }
        public int getRowCount() { return dataValues.length; }
        public Object getValueAt(int row, int col) { return dataValues[row][col]; }
        public String getColumnName(int col) { return columnNames[col]; }
    }

    public void addToBlotter(long orderId, String[] rowData) {
        log.info("adding order data to the blotter...");
        int cols = columnNames.length;
        String[][] newData = new String[dataValues.length + 1][cols];

        // copy existing rows
        for (int i = 0; i < dataValues.length; i++) {
            for (int j = 0; j < cols; j++) {
                newData[i][j] = j < dataValues[i].length ? dataValues[i][j] : "";
            }
        }

        // add new row
        for (int j = 0; j < cols; j++) {
            newData[dataValues.length][j] = j < rowData.length ? rowData[j] : "";
        }

        dataValues = newData;
        table.setModel(new BlotterDataModel());
        applyRenderers();
        update();
        requestFocus();

        // register order's place in the data array
        registerOrder(orderId, dataValues.length - 1);
    }

    public void registerOrder(long orderId, int place) {
        orderPlacement.put(new Long(orderId), new Integer(place));
    }

    public void updateOrderStatus(long orderId, String newStatus) {
        try {
            int pos = ((Integer) orderPlacement.get(new Long(orderId))).intValue();
            log.info("updating order " + orderId + " at position " + pos);
            dataValues[pos][COL_STATUS] = newStatus;
            table.setModel(new BlotterDataModel());
            applyRenderers();
        } catch (Exception e) {
            log.error("Unable to update order status !!!", e);
        }
    }

    class BlotterCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) {
                c.setForeground(Color.YELLOW);
                return c;
            }

            c.setForeground(table.getForeground());

            String sideVal = table.getValueAt(row, COL_SIDE) != null
                    ? table.getValueAt(row, COL_SIDE).toString().trim() : "";

            String colName = table.getColumnName(column);
            if ("Side".equals(colName) || "Symbol".equals(colName)) {
                if ("BUY".equalsIgnoreCase(sideVal)) {
                    c.setForeground(new Color(0, 200, 0));
                } else {
                    c.setForeground(new Color(255, 80, 80));
                }
            } else if ("Status".equals(colName)) {
                String status = value != null ? value.toString().trim() : "";
                if ("Pending".equalsIgnoreCase(status)) {
                    c.setForeground(Color.YELLOW);
                } else if ("Executed".equalsIgnoreCase(status)) {
                    c.setForeground(new Color(0, 200, 0));
                }
            }

            return c;
        }
    }
}
