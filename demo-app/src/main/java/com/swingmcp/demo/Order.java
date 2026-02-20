package com.swingmcp.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Order extends OrderFrame {

    static final String[] SIDE_COMBO = {"BUY", "SELL", "SHORT"};
    static final String[] ORDER_TYPE_COMBO = {"MARKET", "LIMIT", "STOP", "STOP LIMIT"};
    static final String[] ACCOUNT_COMBO = {"MAIN TRADING", "HEDGE FUND ALPHA", "QUANT STRATEGY", "MOMENTUM DESK"};
    static final String[] TIF_COMBO = {"DAY", "GTC", "IOC", "FOK"};
    static final String[] ROUTE_COMBO = {"SMART", "NYSE", "NASDAQ", "ARCA", "BATS"};

    private String[] benchmarkData;
    //                                0        1      2      3        4          5                6          7
    private String[] orderData = {"AAPL", "BUY", "100", "242.50", "24250.00", "MAIN TRADING", "00:00:00", "Pending"};
    ResourceManager resourceManager = new ResourceManager();
    ComponentManager componentManager = new ComponentManager();
    RequestHandler requestHandler = new RequestHandler();
    AppLogger log;
    private Timer marketDataTimer;
    private int bidSize;
    private int askSize;

    public Order() {
        super();
        setOrderId(resourceManager.getNextOrderId());
        setName("orderFrame_" + getOrderId());
        placeOrder.addActionListener(new OrderAction());

        // Enable/disable limit/stop fields based on order type selection
        orderTypeCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String type = (String) orderTypeCombo.getSelectedItem();
                    if ("LIMIT".equals(type)) {
                        enablePriceSpinner(limitPriceSpinner, true);
                        enablePriceSpinner(stopPriceSpinner, false);
                    } else if ("STOP".equals(type)) {
                        enablePriceSpinner(limitPriceSpinner, false);
                        enablePriceSpinner(stopPriceSpinner, true);
                    } else if ("STOP LIMIT".equals(type)) {
                        enablePriceSpinner(limitPriceSpinner, true);
                        enablePriceSpinner(stopPriceSpinner, true);
                    } else {
                        enablePriceSpinner(limitPriceSpinner, false);
                        enablePriceSpinner(stopPriceSpinner, false);
                    }
                }
            }
        });
    }

    private void enablePriceSpinner(javax.swing.JSpinner spinner, boolean enabled) {
        spinner.setEnabled(enabled);
        javax.swing.JTextField tf = ((javax.swing.JSpinner.NumberEditor) spinner.getEditor()).getTextField();
        if (!enabled) {
            tf.setText("");
        } else {
            spinner.setValue(spinner.getValue()); // refresh display
        }
    }

    private void fillCombo(JComboBox combo, String[] choices) {
        for (int iCount = 0; iCount < choices.length; iCount++)
            combo.addItem(choices[iCount]);
    }

    public void fillControls() {
        // combo boxes
        fillCombo(sideCombo, SIDE_COMBO);
        fillCombo(orderTypeCombo, ORDER_TYPE_COMBO);
        fillCombo(accountCombo, ACCOUNT_COMBO);
        fillCombo(tifCombo, TIF_COMBO);
        fillCombo(routeCombo, ROUTE_COMBO);

        // Blank disabled price spinners initially (MARKET order type)
        enablePriceSpinner(limitPriceSpinner, false);
        enablePriceSpinner(stopPriceSpinner, false);

        // If benchmark data is available, fill from it
        if (benchmarkData != null && benchmarkData.length > 0) {
            String symbol = benchmarkData[0];
            String last = benchmarkData.length > 1 ? benchmarkData[1] : "";

            // title and name include symbol with full firm name
            String fullName = InstrumentPanel.getFullName(symbol);
            setTitle("Order Ticket - " + fullName);
            setName("orderFrame_" + symbol);

            ticketLabel.setText(fullName);

            // Bold + bigger symbol
            Font baseFont = ticketLabel.getFont();
            ticketLabel.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize() + 4f));

            qtySpinner.setValue(100);

            // Prepare order data
            double price = 0;
            try {
                price = Double.parseDouble(last);
            } catch (NumberFormatException e) {
                // ignore
            }
            double value = price * 100;
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timeStr = sdf.format(new Date());

            orderData = new String[] {
                symbol,
                "BUY",
                "100",
                last,
                String.format("%.2f", value),
                "MAIN TRADING",
                timeStr,
                "Pending"
            };

            // Initialize bid/ask sizes
            final Random random = new Random();
            bidSize = (random.nextInt(20) + 1) * 100;
            askSize = (random.nextInt(20) + 1) * 100;

            // Update market data display immediately
            updateMarketDataFields();

            // Clicking bid/ask/last populates limit price (or stop price if limit is disabled)
            java.awt.event.MouseListener priceClickListener = new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    javax.swing.JTextField src = (javax.swing.JTextField) e.getSource();
                    String text = src.getText();
                    if (text == null || text.trim().isEmpty()) return;
                    try {
                        double val = Double.parseDouble(text.trim().replace(",", ""));
                        if (limitPriceSpinner.isEnabled()) {
                            limitPriceSpinner.setValue(val);
                        } else if (stopPriceSpinner.isEnabled()) {
                            stopPriceSpinner.setValue(val);
                        }
                    } catch (NumberFormatException ex) {
                        // ignore non-numeric fields
                    }
                }
            };
            bidField.addMouseListener(priceClickListener);
            lastField.addMouseListener(priceClickListener);
            askField.addMouseListener(priceClickListener);

            // Clicking bid size / ask size populates the quantity spinner
            java.awt.event.MouseListener sizeClickListener = new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    javax.swing.JTextField src = (javax.swing.JTextField) e.getSource();
                    String text = src.getText();
                    if (text == null || text.trim().isEmpty()) return;
                    try {
                        int val = Integer.parseInt(text.trim().replace(",", ""));
                        qtySpinner.setValue(val);
                    } catch (NumberFormatException ex) {
                        // ignore non-numeric fields
                    }
                }
            };
            bidSizeField.addMouseListener(sizeClickListener);
            askSizeField.addMouseListener(sizeClickListener);

            // Ticking market data timer - reads from shared benchmarkData array
            // (which is updated by QuotePanel's timer when panel is active)
            marketDataTimer = new Timer(333, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // Randomly adjust bid/ask sizes
                    int sizeDelta = random.nextInt(3) - 1; // -1, 0, or +1
                    bidSize = Math.max(100, bidSize + sizeDelta * 100);
                    sizeDelta = random.nextInt(3) - 1;
                    askSize = Math.max(100, askSize + sizeDelta * 100);

                    updateMarketDataFields();
                }
            });
            marketDataTimer.start();
        }

        // Enhanced Order Summary with all 9 fields (no "Order Summary:" prefix)
        String side = (String) sideCombo.getSelectedItem();
        String qty = String.valueOf(qtySpinner.getValue());
        String sym = orderData[0];
        String orderType = (String) orderTypeCombo.getSelectedItem();
        String limitPrice = String.valueOf(limitPriceSpinner.getValue());
        String stopPrice = String.valueOf(stopPriceSpinner.getValue());
        String account = (String) accountCombo.getSelectedItem();
        String tif = (String) tifCombo.getSelectedItem();
        String route = (String) routeCombo.getSelectedItem();

        orderSummaryLabel.setText("<html>"
            + "Side: <b>" + (side != null ? side : orderData[1]) + "</b><br/>"
            + "Quantity: <b>" + (qty != null && !qty.isEmpty() ? qty : orderData[2]) + "</b><br/>"
            + "Symbol: <b>" + sym + "</b><br/>"
            + "Price Type: <b>" + (orderType != null ? orderType : "MARKET") + "</b><br/>"
            + "Limit Price: <b>" + (limitPrice != null ? limitPrice : "") + "</b><br/>"
            + "Stop Price: <b>" + (stopPrice != null ? stopPrice : "") + "</b><br/>"
            + "Account: <b>" + (account != null ? account : orderData[5]) + "</b><br/>"
            + "Time in Force: <b>" + (tif != null ? tif : "DAY") + "</b><br/>"
            + "Route: <b>" + (route != null ? route : "SMART") + "</b>"
            + "</html>");

        riskLabel.setText("<html>Risk Analysis:<br/>Position Exposure: $" + orderData[4]
            + "<br/>Account: " + orderData[5] + "</html>");

        // Focus on Side field when ticket opens
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                sideCombo.requestFocusInWindow();
            }
        });
    }

    private void updateMarketDataFields() {
        // Read live values from benchmarkData (shared with QuotePanel)
        // Columns: 0=Symbol, 1=Last, 2=Change, 3=Change%, 4=Bid, 5=Ask
        bidField.setText(benchmarkData.length > 4 ? benchmarkData[4] : "");
        bidSizeField.setText(String.valueOf(bidSize));
        lastField.setText(benchmarkData.length > 1 ? benchmarkData[1] : "");
        askSizeField.setText(String.valueOf(askSize));
        askField.setText(benchmarkData.length > 5 ? benchmarkData[5] : "");
    }

    public void dispose() {
        if (marketDataTimer != null) {
            marketDataTimer.stop();
        }
        super.dispose();
    }

    private long m_OrderId;
    public void setOrderId(long id) {
        m_OrderId = id;
    }
    public long getOrderId() {
        return m_OrderId;
    }

    private String m_OrderType;
    public void setOrderType(String type) {
        m_OrderType = type;
    }
    public String getOrderType() {
        return m_OrderType;
    }

    public void changeColor(Color color) {
        getContentPane().setBackground(color);
    }

    public void setBenchmarkData(String[] data) {
        benchmarkData = data;
    }
    public String[] getBenchmarkData() {
        return benchmarkData;
    }

    class OrderAction implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            log.info("initiating order placement...");
            try {
                Object object = event.getSource();
                if (object == placeOrder) {
                    // Commit any pending spinner edits
                    try { qtySpinner.commitEdit(); } catch (java.text.ParseException pe) {}

                    // Update order data from current field values
                    String side = (String) sideCombo.getSelectedItem();
                    String qty = String.valueOf(qtySpinner.getValue());
                    String account = (String) accountCombo.getSelectedItem();

                    if (side != null) orderData[1] = side;
                    if (qty != null && !qty.trim().isEmpty()) orderData[2] = qty;
                    if (account != null) orderData[5] = account;

                    // Commit any pending edits in price spinners
                    try { limitPriceSpinner.commitEdit(); } catch (java.text.ParseException pe) {}
                    try { stopPriceSpinner.commitEdit(); } catch (java.text.ParseException pe) {}

                    // Use limit price for LIMIT/STOP LIMIT orders, stop price for STOP orders
                    String orderType = (String) orderTypeCombo.getSelectedItem();
                    if ("LIMIT".equals(orderType) || "STOP LIMIT".equals(orderType)) {
                        String limitPrice = String.valueOf(limitPriceSpinner.getValue());
                        if (limitPrice != null && !limitPrice.trim().isEmpty()) {
                            orderData[3] = limitPrice.trim();
                        }
                    } else if ("STOP".equals(orderType)) {
                        String stopPrice = String.valueOf(stopPriceSpinner.getValue());
                        if (stopPrice != null && !stopPrice.trim().isEmpty()) {
                            orderData[3] = stopPrice.trim();
                        }
                    }

                    // Recalculate value
                    try {
                        double price = Double.parseDouble(orderData[3]);
                        double quantity = Double.parseDouble(orderData[2]);
                        orderData[4] = String.format("%.2f", price * quantity);
                    } catch (NumberFormatException e) {
                        // keep existing value
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    orderData[6] = sdf.format(new Date());
                    orderData[7] = "Pending";

                    // Set border color based on side: green for BUY, red for SELL/SHORT
                    Color borderColor = "BUY".equals(side) ? new Color(0, 200, 0) : new Color(255, 80, 80);
                    getRootPane().setBorder(BorderFactory.createLineBorder(borderColor, 3));
                    revalidate();

                    // Build blotter row matching column order:
                    // Time, Status, Side, Qty, Symbol, Order Type, Limit Price, TIF, Route, Account, Value
                    String tif = (String) tifCombo.getSelectedItem();
                    String route = (String) routeCombo.getSelectedItem();
                    String blotterLimitPrice = "";
                    if ("LIMIT".equals(orderType) || "STOP LIMIT".equals(orderType)) {
                        blotterLimitPrice = String.valueOf(limitPriceSpinner.getValue());
                    }
                    String[] blotterRow = {
                        orderData[6],                                  // Time
                        orderData[7],                                  // Status
                        orderData[1],                                  // Side
                        orderData[2],                                  // Qty
                        orderData[0],                                  // Symbol
                        orderType != null ? orderType : "MARKET",      // Order Type
                        blotterLimitPrice,                             // Limit Price
                        tif != null ? tif : "DAY",                     // Time In Force
                        route != null ? route : "SMART",               // Route
                        orderData[5],                                  // Account
                        orderData[4]                                   // Value
                    };
                    componentManager.getBlotterPanel().addToBlotter(getOrderId(), blotterRow);
                    log.info("Order has been added to the blotter...");
                    componentManager.showBlotter();
                    requestFocus();
                    registerOrder();
                }
            } catch (Exception e) {
                log.error("Exception ", e);
            }
        }
    }

    public void registerOrder() {
        requestHandler.registerOrder(this);
        Color statusColor = "BUY".equals(orderData[1]) ? new Color(0, 200, 0) : new Color(255, 80, 80);
        log.showStatus("Your order is waiting to be filled", statusColor);
    }

    public void executeOrder() {
        log.showStatus("Order " + getOrderId() + " executed: " + orderData[1] + " "
            + orderData[2] + " " + orderData[0] + " at " + orderData[3], Color.green);

        // Close the order ticket after execution
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setVisible(false);
                dispose();
            }
        });
    }
}
