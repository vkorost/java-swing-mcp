package com.swingmcp.demo;

import com.swingmcp.server.SwingMcpServer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Demo trading blotter application for SwingMCP testing.
 *
 * <p>Provides a rich set of Swing components (table, tree, combo boxes,
 * text fields, menus, checkboxes) that exercise all MCP server capabilities.
 * Includes an intentional dark-mode contrast bug for contrast checker validation.</p>
 */
public class TradingBlotterApp {

    private JFrame frame;
    private JTextField symbolField;
    private JComboBox<String> sideCombo;
    private JTextField qtyField;
    private JComboBox<String> accountCombo;
    private JTextField priceField;
    private JButton submitButton;
    private JButton clearButton;
    private JCheckBox limitCheckbox;
    private JTable ordersTable;
    private DefaultTableModel tableModel;
    private JTree portfolioTree;
    private JLabel ordersCountLabel;
    private JLabel filledCountLabel;
    private JLabel workingCountLabel;
    private JLabel rejectedCountLabel;
    private JLabel lastActionLabel;
    private boolean darkMode = false;

    // Status panel reference for dark mode contrast bug
    private JPanel statusPanel;

    public static void main(String[] args) {
        // Set Nimbus L&F
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Fall back to default
        }

        SwingUtilities.invokeLater(() -> {
            TradingBlotterApp app = new TradingBlotterApp();
            app.createAndShowGUI();

            // Start MCP server after UI is ready
            SwingMcpServer.start(9222);
            System.out.println("SwingMCP Server running on http://localhost:9222");
        });
    }

    private void createAndShowGUI() {
        frame = new JFrame("SwingMCP Demo - Trading Blotter");
        frame.setName("mainFrame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLayout(new BorderLayout(5, 5));

        // Menu bar
        frame.setJMenuBar(createMenuBar());

        // Order entry panel (top)
        frame.add(createOrderEntryPanel(), BorderLayout.NORTH);

        // Orders table (center)
        frame.add(createOrdersTablePanel(), BorderLayout.CENTER);

        // Bottom panel: portfolio tree + status
        frame.add(createBottomPanel(), BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setName("menuBar");

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setName("fileMenu");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setName("editMenu");
        editMenu.add(new JMenuItem("Copy"));
        editMenu.add(new JMenuItem("Paste"));

        // View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setName("viewMenu");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> updateStatusLabels());
        JMenuItem darkModeItem = new JMenuItem("Dark Mode");
        darkModeItem.addActionListener(e -> toggleDarkMode());
        viewMenu.add(refreshItem);
        viewMenu.add(darkModeItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setName("helpMenu");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e ->
                JOptionPane.showMessageDialog(frame, "SwingMCP Demo v1.0", "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JPanel createOrderEntryPanel() {
        JPanel panel = new JPanel();
        panel.setName("orderEntryPanel");
        panel.setBorder(BorderFactory.createTitledBorder("Order Entry"));
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: Symbol, Side, Qty
        gbc.gridy = 0;
        gbc.gridx = 0;
        panel.add(new JLabel("Symbol:"), gbc);
        gbc.gridx = 1;
        symbolField = new JTextField(10);
        symbolField.setName("symbolField");
        panel.add(symbolField, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Side:"), gbc);
        gbc.gridx = 3;
        sideCombo = new JComboBox<>(new String[]{"BUY", "SELL", "SHORT"});
        sideCombo.setName("sideCombo");
        panel.add(sideCombo, gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("Qty:"), gbc);
        gbc.gridx = 5;
        qtyField = new JTextField(8);
        qtyField.setName("qtyField");
        panel.add(qtyField, gbc);

        // Row 2: Account, Price
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Account:"), gbc);
        gbc.gridx = 1;
        accountCombo = new JComboBox<>(new String[]{"ACCT1", "ACCT2", "ACCT3", "ACCT4"});
        accountCombo.setName("accountCombo");
        panel.add(accountCombo, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 3;
        priceField = new JTextField(10);
        priceField.setName("priceField");
        priceField.setEnabled(false);
        panel.add(priceField, gbc);

        // Row 3: Buttons and checkbox
        gbc.gridy = 2;
        gbc.gridx = 0;
        submitButton = new JButton("Submit Order");
        submitButton.setName("submitButton");
        submitButton.addActionListener(this::onSubmitOrder);
        panel.add(submitButton, gbc);

        gbc.gridx = 1;
        clearButton = new JButton("Clear");
        clearButton.setName("clearButton");
        clearButton.addActionListener(e -> clearFields());
        panel.add(clearButton, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        limitCheckbox = new JCheckBox("Limit Order");
        limitCheckbox.setName("limitCheckbox");
        limitCheckbox.addActionListener(e -> {
            priceField.setEnabled(limitCheckbox.isSelected());
            if (!limitCheckbox.isSelected()) {
                priceField.setText("");
            }
        });
        panel.add(limitCheckbox, gbc);

        return panel;
    }

    private JScrollPane createOrdersTablePanel() {
        String[] columns = {"Symbol", "Side", "Qty", "Price", "Status", "Account"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Pre-populate with 15 sample rows
        Object[][] sampleData = {
                {"AAPL", "BUY", 100, 185.50, "Working", "ACCT1"},
                {"MSFT", "SELL", 200, 420.10, "Filled", "ACCT2"},
                {"GOOGL", "BUY", 50, 176.80, "Rejected", "ACCT1"},
                {"TSLA", "BUY", 150, 248.30, "Filled", "ACCT3"},
                {"AMZN", "SELL", 75, 185.90, "Working", "ACCT2"},
                {"NVDA", "BUY", 300, 875.40, "Filled", "ACCT1"},
                {"META", "SELL", 120, 505.60, "Filled", "ACCT4"},
                {"JPM", "BUY", 250, 198.20, "Working", "ACCT2"},
                {"V", "SELL", 80, 280.70, "Filled", "ACCT3"},
                {"JNJ", "BUY", 175, 155.40, "Filled", "ACCT1"},
                {"WMT", "BUY", 90, 168.50, "Working", "ACCT4"},
                {"PG", "SELL", 110, 162.30, "Filled", "ACCT2"},
                {"UNH", "BUY", 60, 525.80, "Rejected", "ACCT3"},
                {"HD", "SELL", 45, 378.90, "Working", "ACCT1"},
                {"DIS", "BUY", 200, 112.40, "Filled", "ACCT4"}
        };

        for (Object[] row : sampleData) {
            tableModel.addRow(row);
        }

        ordersTable = new JTable(tableModel);
        ordersTable.setName("ordersTable");
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // When a row is selected, populate input fields
        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = ordersTable.getSelectedRow();
                if (row >= 0) {
                    symbolField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
                    sideCombo.setSelectedItem(String.valueOf(tableModel.getValueAt(row, 1)));
                    qtyField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
                    Object price = tableModel.getValueAt(row, 3);
                    priceField.setText(String.valueOf(price));
                    accountCombo.setSelectedItem(String.valueOf(tableModel.getValueAt(row, 5)));
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.setName("ordersTableScroll");
        scrollPane.setBorder(BorderFactory.createTitledBorder("Orders"));
        return scrollPane;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        bottomPanel.setPreferredSize(new Dimension(0, 200));

        // Portfolio tree
        bottomPanel.add(createPortfolioTree());

        // Status panel
        bottomPanel.add(createStatusPanel());

        return bottomPanel;
    }

    private JScrollPane createPortfolioTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Portfolio");

        // Equities
        DefaultMutableTreeNode equities = new DefaultMutableTreeNode("Equities");
        equities.add(new DefaultMutableTreeNode("AAPL"));
        equities.add(new DefaultMutableTreeNode("MSFT"));
        equities.add(new DefaultMutableTreeNode("GOOGL"));
        equities.add(new DefaultMutableTreeNode("TSLA"));
        equities.add(new DefaultMutableTreeNode("AMZN"));
        root.add(equities);

        // Futures
        DefaultMutableTreeNode futures = new DefaultMutableTreeNode("Futures");
        futures.add(new DefaultMutableTreeNode("ESH5"));
        futures.add(new DefaultMutableTreeNode("NQH5"));
        root.add(futures);

        // Options
        DefaultMutableTreeNode options = new DefaultMutableTreeNode("Options");
        options.add(new DefaultMutableTreeNode("AAPL240119C"));
        options.add(new DefaultMutableTreeNode("SPY240119P"));
        root.add(options);

        portfolioTree = new JTree(new DefaultTreeModel(root));
        portfolioTree.setName("portfolioTree");

        // Expand all nodes
        for (int i = 0; i < portfolioTree.getRowCount(); i++) {
            portfolioTree.expandRow(i);
        }

        // When a leaf node is selected, populate symbol field
        portfolioTree.addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.isLeaf()) {
                    symbolField.setText(node.getUserObject().toString());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(portfolioTree);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Portfolio Tree"));
        return scrollPane;
    }

    private JPanel createStatusPanel() {
        statusPanel = new JPanel(new GridLayout(5, 1, 0, 4));
        statusPanel.setName("statusPanel");
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status Panel"));

        ordersCountLabel = new JLabel("Orders: 15");
        ordersCountLabel.setName("ordersCountLabel");
        filledCountLabel = new JLabel("Filled: 8");
        filledCountLabel.setName("filledCountLabel");
        workingCountLabel = new JLabel("Working: 5");
        workingCountLabel.setName("workingCountLabel");
        rejectedCountLabel = new JLabel("Rejected: 2");
        rejectedCountLabel.setName("rejectedCountLabel");
        lastActionLabel = new JLabel("Last Action: -");
        lastActionLabel.setName("lastActionLabel");

        statusPanel.add(ordersCountLabel);
        statusPanel.add(filledCountLabel);
        statusPanel.add(workingCountLabel);
        statusPanel.add(rejectedCountLabel);
        statusPanel.add(lastActionLabel);

        return statusPanel;
    }

    private void onSubmitOrder(ActionEvent e) {
        String symbol = symbolField.getText().trim();
        String side = (String) sideCombo.getSelectedItem();
        String qtyText = qtyField.getText().trim();
        String account = (String) accountCombo.getSelectedItem();
        String priceText = priceField.getText().trim();

        // Validation
        if (symbol.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Symbol is required", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (qtyText.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Quantity is required", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Quantity must be a number", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double price = 0.0;
        if (limitCheckbox.isSelected()) {
            if (priceText.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Price is required for limit orders", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                price = Double.parseDouble(priceText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Price must be a number", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Add row to table
        tableModel.addRow(new Object[]{symbol, side, qty, price, "Working", account});

        // Update status
        lastActionLabel.setText("Last Action: Submit " + symbol);
        updateStatusLabels();

        // Clear fields
        clearFields();
    }

    private void clearFields() {
        symbolField.setText("");
        qtyField.setText("");
        priceField.setText("");
        sideCombo.setSelectedIndex(0);
        accountCombo.setSelectedIndex(0);
        limitCheckbox.setSelected(false);
        priceField.setEnabled(false);
    }

    private void updateStatusLabels() {
        int total = tableModel.getRowCount();
        int filled = 0, working = 0, rejected = 0;

        for (int i = 0; i < total; i++) {
            String status = String.valueOf(tableModel.getValueAt(i, 4));
            switch (status) {
                case "Filled": filled++; break;
                case "Working": working++; break;
                case "Rejected": rejected++; break;
            }
        }

        ordersCountLabel.setText("Orders: " + total);
        filledCountLabel.setText("Filled: " + filled);
        workingCountLabel.setText("Working: " + working);
        rejectedCountLabel.setText("Rejected: " + rejected);
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;

        if (darkMode) {
            // Apply dark theme overrides
            Color darkBg = new Color(50, 50, 50);
            Color darkFg = new Color(220, 220, 220);
            Color darkFieldBg = new Color(70, 70, 70);

            frame.getContentPane().setBackground(darkBg);
            applyDarkTheme(frame.getContentPane(), darkBg, darkFg, darkFieldBg);

            // INTENTIONAL CONTRAST BUG: Status panel labels use very low contrast
            Color bugForeground = new Color(60, 60, 60);  // Nearly same as background
            ordersCountLabel.setForeground(bugForeground);
            filledCountLabel.setForeground(bugForeground);
            workingCountLabel.setForeground(bugForeground);
            rejectedCountLabel.setForeground(bugForeground);
            lastActionLabel.setForeground(bugForeground);

            statusPanel.setBackground(darkBg);
            statusPanel.setOpaque(true);
            ordersCountLabel.setOpaque(true);
            filledCountLabel.setOpaque(true);
            workingCountLabel.setOpaque(true);
            rejectedCountLabel.setOpaque(true);
            lastActionLabel.setOpaque(true);
            ordersCountLabel.setBackground(darkBg);
            filledCountLabel.setBackground(darkBg);
            workingCountLabel.setBackground(darkBg);
            rejectedCountLabel.setBackground(darkBg);
            lastActionLabel.setBackground(darkBg);

        } else {
            // Reset to default Nimbus theme
            resetTheme(frame.getContentPane());

            // Reset status labels
            ordersCountLabel.setForeground(null);
            filledCountLabel.setForeground(null);
            workingCountLabel.setForeground(null);
            rejectedCountLabel.setForeground(null);
            lastActionLabel.setForeground(null);

            ordersCountLabel.setOpaque(false);
            filledCountLabel.setOpaque(false);
            workingCountLabel.setOpaque(false);
            rejectedCountLabel.setOpaque(false);
            lastActionLabel.setOpaque(false);
            statusPanel.setOpaque(false);
        }

        SwingUtilities.updateComponentTreeUI(frame);
        frame.repaint();
    }

    private void applyDarkTheme(Container container, Color bg, Color fg, Color fieldBg) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                panel.setBackground(bg);
                panel.setForeground(fg);
                panel.setOpaque(true);
            } else if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(fg);
            } else if (comp instanceof JTextField) {
                JTextField field = (JTextField) comp;
                field.setBackground(fieldBg);
                field.setForeground(fg);
                field.setCaretColor(fg);
            } else if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setBackground(new Color(80, 80, 80));
                button.setForeground(fg);
            } else if (comp instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) comp;
                cb.setBackground(bg);
                cb.setForeground(fg);
            } else if (comp instanceof JComboBox) {
                JComboBox<?> combo = (JComboBox<?>) comp;
                combo.setBackground(fieldBg);
                combo.setForeground(fg);
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                scrollPane.setBackground(bg);
                scrollPane.getViewport().setBackground(bg);
                Component view = scrollPane.getViewport().getView();
                if (view instanceof JTable) {
                    JTable table = (JTable) view;
                    table.setBackground(new Color(60, 60, 60));
                    table.setForeground(fg);
                    table.setGridColor(new Color(90, 90, 90));
                    table.getTableHeader().setBackground(new Color(70, 70, 70));
                    table.getTableHeader().setForeground(fg);
                } else if (view instanceof JTree) {
                    JTree tree = (JTree) view;
                    tree.setBackground(new Color(60, 60, 60));
                    tree.setForeground(fg);
                }
            }

            if (comp instanceof Container) {
                applyDarkTheme((Container) comp, bg, fg, fieldBg);
            }
        }
    }

    private void resetTheme(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JComponent) {
                JComponent jc = (JComponent) comp;
                jc.setBackground(null);
                jc.setForeground(null);
                jc.setOpaque(false);
            }
            if (comp instanceof JTextField) {
                ((JTextField) comp).setCaretColor(null);
            }
            if (comp instanceof Container) {
                resetTheme((Container) comp);
            }
        }
    }
}
