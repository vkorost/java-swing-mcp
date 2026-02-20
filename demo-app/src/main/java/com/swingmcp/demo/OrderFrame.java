package com.swingmcp.demo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;

public class OrderFrame extends JInternalFrame {

    public OrderFrame() {
        setTitle("Order Ticket");
        setToolTipText("Enter new order here...");
        setIconifiable(true);
        setResizable(true);
        setMinimumSize(new Dimension(600, 595));
        getContentPane().setLayout(null);
        setSize(600, 595);

        // Ticket label (instrument name)
        ticketLabel.setText("Order Ticket");
        ticketLabel.setName("ticketLabel");
        getContentPane().add(ticketLabel);
        ticketLabel.setBounds(12, 12, 396, 18);

        // Market data row: labels at y=42, fields at y=56 (12px gap after symbol)
        // Aligned with Side/Qty/OrderType/LimitPrice/StopPrice columns below
        JLabel bidLabel = new JLabel("Bid");
        getContentPane().add(bidLabel);
        bidLabel.setBounds(12, 42, 120, 12);

        bidField.setName("orderBidField");
        bidField.setEditable(false);
        bidField.setHorizontalAlignment(JTextField.RIGHT);
        bidField.setForeground(new Color(0, 200, 0));
        bidField.setFocusable(false);
        getContentPane().add(bidField);
        bidField.setBounds(12, 56, 120, 25);

        JLabel bidSizeLabel = new JLabel("Bid Size");
        getContentPane().add(bidSizeLabel);
        bidSizeLabel.setBounds(144, 42, 100, 12);

        bidSizeField.setName("orderBidSizeField");
        bidSizeField.setEditable(false);
        bidSizeField.setHorizontalAlignment(JTextField.RIGHT);
        bidSizeField.setForeground(new Color(0, 200, 0));
        bidSizeField.setFocusable(false);
        getContentPane().add(bidSizeField);
        bidSizeField.setBounds(144, 56, 100, 25);

        JLabel lastLabel = new JLabel("Last");
        getContentPane().add(lastLabel);
        lastLabel.setBounds(256, 42, 120, 12);

        lastField.setName("orderLastField");
        lastField.setEditable(false);
        lastField.setHorizontalAlignment(JTextField.RIGHT);
        lastField.setForeground(new Color(218, 165, 32));
        lastField.setFocusable(false);
        getContentPane().add(lastField);
        lastField.setBounds(256, 56, 120, 25);

        JLabel askSizeLabel = new JLabel("Ask Size");
        getContentPane().add(askSizeLabel);
        askSizeLabel.setBounds(388, 42, 80, 12);

        askSizeField.setName("orderAskSizeField");
        askSizeField.setEditable(false);
        askSizeField.setHorizontalAlignment(JTextField.RIGHT);
        askSizeField.setForeground(new Color(255, 80, 80));
        askSizeField.setFocusable(false);
        getContentPane().add(askSizeField);
        askSizeField.setBounds(388, 56, 80, 25);

        JLabel askLabel = new JLabel("Ask");
        getContentPane().add(askLabel);
        askLabel.setBounds(480, 42, 80, 12);

        askField.setName("orderAskField");
        askField.setEditable(false);
        askField.setHorizontalAlignment(JTextField.RIGHT);
        askField.setForeground(new Color(255, 80, 80));
        askField.setFocusable(false);
        getContentPane().add(askField);
        askField.setBounds(480, 56, 80, 25);

        // Side (12px gap after MD row)
        JLabel sideLabel = new JLabel("Side");
        getContentPane().add(sideLabel);
        sideLabel.setBounds(12, 93, 60, 12);

        sideCombo.setName("orderSideCombo");
        getContentPane().add(sideCombo);
        sideCombo.setBounds(12, 107, 120, 25);

        // Quantity (spinner, step 100)
        JLabel qtyLabel = new JLabel("Quantity");
        getContentPane().add(qtyLabel);
        qtyLabel.setBounds(144, 93, 60, 12);

        qtySpinner.setName("orderQtySpinner");
        qtySpinner.setToolTipText("Enter quantity");
        ((JSpinner.NumberEditor) qtySpinner.getEditor()).getTextField().setName("orderQtyField");
        getContentPane().add(qtySpinner);
        qtySpinner.setBounds(144, 107, 100, 25);

        // Order Type
        JLabel orderTypeLabel = new JLabel("Order Type");
        getContentPane().add(orderTypeLabel);
        orderTypeLabel.setBounds(256, 93, 72, 12);

        orderTypeCombo.setName("orderTypeCombo");
        getContentPane().add(orderTypeCombo);
        orderTypeCombo.setBounds(256, 107, 120, 25);

        // Limit Price (spinner, step 0.01)
        JLabel limitPriceLabel = new JLabel("Limit Price");
        getContentPane().add(limitPriceLabel);
        limitPriceLabel.setBounds(388, 93, 72, 12);

        limitPriceSpinner.setName("orderLimitPrice");
        limitPriceSpinner.setEnabled(false);
        JSpinner.NumberEditor limitEditor = (JSpinner.NumberEditor) limitPriceSpinner.getEditor();
        limitEditor.getFormat().setMinimumFractionDigits(2);
        limitEditor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        getContentPane().add(limitPriceSpinner);
        limitPriceSpinner.setBounds(388, 107, 80, 25);

        // Stop Price (spinner, step 0.01)
        JLabel stopPriceLabel = new JLabel("Stop Price");
        getContentPane().add(stopPriceLabel);
        stopPriceLabel.setBounds(480, 93, 72, 12);

        stopPriceSpinner.setName("orderStopPrice");
        stopPriceSpinner.setEnabled(false);
        JSpinner.NumberEditor stopEditor = (JSpinner.NumberEditor) stopPriceSpinner.getEditor();
        stopEditor.getFormat().setMinimumFractionDigits(2);
        stopEditor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        getContentPane().add(stopPriceSpinner);
        stopPriceSpinner.setBounds(480, 107, 80, 25);

        // Account
        JLabel accountLabel = new JLabel("Account");
        getContentPane().add(accountLabel);
        accountLabel.setBounds(12, 139, 60, 12);

        accountCombo.setName("orderAccountCombo");
        getContentPane().add(accountCombo);
        accountCombo.setBounds(12, 153, 200, 25);

        // Time in Force
        JLabel tifLabel = new JLabel("Time in Force");
        getContentPane().add(tifLabel);
        tifLabel.setBounds(224, 139, 84, 12);

        tifCombo.setName("orderTifCombo");
        getContentPane().add(tifCombo);
        tifCombo.setBounds(224, 153, 100, 25);

        // Route
        JLabel routeLabel = new JLabel("Route");
        getContentPane().add(routeLabel);
        routeLabel.setBounds(336, 139, 48, 12);

        routeCombo.setName("orderRouteCombo");
        getContentPane().add(routeCombo);
        routeCombo.setBounds(336, 153, 100, 25);

        // Tabbed Panel
        tabPanel.setName("orderTabPanel");
        getContentPane().add(tabPanel);
        tabPanel.setBounds(12, 191, 552, 220);

        // Tab: Order
        JPanel orderTab = new JPanel();
        orderTab.setLayout(null);
        orderSummaryLabel.setBounds(10, 10, 520, 180);
        orderTab.add(orderSummaryLabel);
        tabPanel.addTab("Order", orderTab);

        // Tab: Allocation
        tabPanel.addTab("Allocation", new JPanel());

        // Tab: Risk
        JPanel riskTab = new JPanel();
        riskTab.setLayout(null);
        riskLabel.setBounds(10, 10, 520, 160);
        riskTab.add(riskLabel);
        tabPanel.addTab("Risk", riskTab);

        // Tab: History
        tabPanel.addTab("History", new JPanel());

        tabPanel.setSelectedIndex(0);

        // Notes label
        JLabel notesLabel = new JLabel("Notes / Comments");
        getContentPane().add(notesLabel);
        notesLabel.setBounds(12, 419, 120, 12);

        bottomNotesArea.setName("orderBottomNotesArea");
        getContentPane().add(bottomNotesArea);
        bottomNotesArea.setBounds(12, 433, 552, 50);

        // Buttons
        placeOrder.setName("placeOrderButton");
        placeOrder.setToolTipText("Place order");
        placeOrder.setText("Place Order");
        placeOrder.setActionCommand("Place Order");
        placeOrder.setFont(placeOrder.getFont().deriveFont(Font.BOLD));
        getContentPane().add(placeOrder);
        placeOrder.setBounds(12, 491, 130, 24);

        closeButton.setName("closeButton");
        closeButton.setToolTipText("Cancel Order Ticket");
        closeButton.setText("Cancel");
        closeButton.setActionCommand("Cancel");
        getContentPane().add(closeButton);
        closeButton.setBounds(446, 491, 130, 24);

        // Register button listeners
        SymAction lSymAction = new SymAction();
        placeOrder.addActionListener(lSymAction);
        closeButton.addActionListener(lSymAction);

        // ESC key binding to close the ticket
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeTicket");
        getRootPane().getActionMap().put("closeTicket", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
    }

    public OrderFrame(String sTitle) {
        this();
        setTitle(sTitle);
    }

    public void addNotify() {
        Dimension size = getSize();
        super.addNotify();
        if (frameSizeAdjusted)
            return;
        frameSizeAdjusted = true;
        Insets insets = getInsets();
        javax.swing.JMenuBar menuBar = getRootPane().getJMenuBar();
        int menuBarHeight = 0;
        if (menuBar != null)
            menuBarHeight = menuBar.getPreferredSize().height;
        int offset = 0;
        Component comp[] = getComponents();
        for (int i = 0; i < comp.length; ++i) {
            if (comp[i] instanceof javax.swing.JRootPane)
                continue;
            offset += comp[i].getPreferredSize().height;
        }
        setSize(insets.left + insets.right + size.width,
                insets.top + insets.bottom + size.height + menuBarHeight + offset);
    }

    boolean frameSizeAdjusted = false;

    // Controls
    JButton placeOrder = new JButton();
    JButton closeButton = new JButton();
    JComboBox sideCombo = new JComboBox();
    JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(100, 100, 999999900, 100));
    JComboBox orderTypeCombo = new JComboBox();
    JSpinner limitPriceSpinner = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 999999.99, 0.01));
    JSpinner stopPriceSpinner = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 999999.99, 0.01));
    JComboBox accountCombo = new JComboBox();
    JComboBox tifCombo = new JComboBox();
    JComboBox routeCombo = new JComboBox();
    JTabbedPane tabPanel = new JTabbedPane();
    JLabel ticketLabel = new JLabel();
    JTextField bidField = new JTextField();
    JTextField bidSizeField = new JTextField();
    JTextField lastField = new JTextField();
    JTextField askSizeField = new JTextField();
    JTextField askField = new JTextField();
    JLabel orderSummaryLabel = new JLabel();
    JLabel riskLabel = new JLabel();
    JTextArea bottomNotesArea = new JTextArea();

    class SymAction implements java.awt.event.ActionListener {
        public void actionPerformed(java.awt.event.ActionEvent event) {
            Object object = event.getSource();
            if (object == placeOrder)
                placeOrder_actionPerformed(event);
            else if (object == closeButton)
                closeButton_actionPerformed(event);
        }
    }

    void placeOrder_actionPerformed(java.awt.event.ActionEvent event) {
        placeOrder_actionPerformed_Interaction1(event);
    }

    void placeOrder_actionPerformed_Interaction1(java.awt.event.ActionEvent event) {
        try {
            placeOrder.setEnabled(false);
        } catch (Exception e) {
        }
    }

    void closeButton_actionPerformed(java.awt.event.ActionEvent event) {
        closeButton_actionPerformed_Interaction1(event);
    }

    void closeButton_actionPerformed_Interaction1(java.awt.event.ActionEvent event) {
        try {
            this.setVisible(false);
        } catch (Exception e) {
        }
    }
}
