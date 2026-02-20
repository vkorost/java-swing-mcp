package com.swingmcp.demo;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.HashMap;

public class InstrumentPanel extends AbstractPanel
        implements TreeSelectionListener, KeyListener, MouseListener {

    private JTree tree;
    private JScrollPane scrollPane;
    private ComponentManager componentManager = new ComponentManager();
    private Timer singleClickTimer;

    AppLogger log;

    private static final HashMap SYMBOL_NAMES = new HashMap();
    static {
        SYMBOL_NAMES.put("AAPL", "Apple Inc");
        SYMBOL_NAMES.put("MSFT", "Microsoft Corp");
        SYMBOL_NAMES.put("GOOGL", "Alphabet Inc");
        SYMBOL_NAMES.put("NVDA", "NVIDIA Corp");
        SYMBOL_NAMES.put("META", "Meta Platforms");
        SYMBOL_NAMES.put("AMZN", "Amazon.com Inc");
        SYMBOL_NAMES.put("TSLA", "Tesla Inc");
        SYMBOL_NAMES.put("AMD", "Advanced Micro Devices");
        SYMBOL_NAMES.put("JPM", "JPMorgan Chase");
        SYMBOL_NAMES.put("BAC", "Bank of America");
        SYMBOL_NAMES.put("GS", "Goldman Sachs");
        SYMBOL_NAMES.put("MS", "Morgan Stanley");
        SYMBOL_NAMES.put("WFC", "Wells Fargo");
        SYMBOL_NAMES.put("C", "Citigroup");
        SYMBOL_NAMES.put("JNJ", "Johnson & Johnson");
        SYMBOL_NAMES.put("UNH", "UnitedHealth Group");
        SYMBOL_NAMES.put("PFE", "Pfizer Inc");
        SYMBOL_NAMES.put("ABBV", "AbbVie Inc");
        SYMBOL_NAMES.put("MRK", "Merck & Co");
        SYMBOL_NAMES.put("XOM", "ExxonMobil");
        SYMBOL_NAMES.put("CVX", "Chevron Corp");
        SYMBOL_NAMES.put("COP", "ConocoPhillips");
        SYMBOL_NAMES.put("SLB", "Schlumberger");
        SYMBOL_NAMES.put("SPY", "SPDR S&P 500");
        SYMBOL_NAMES.put("QQQ", "Invesco QQQ Trust");
        SYMBOL_NAMES.put("IWM", "iShares Russell 2000");
        SYMBOL_NAMES.put("DIA", "SPDR Dow Jones");
    }

    public static String getFullName(String symbol) {
        String name = (String) SYMBOL_NAMES.get(symbol);
        return name != null ? symbol + " - " + name : symbol;
    }

    public void populatePanel() {
        this.setLayout(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");

        DefaultMutableTreeNode technology = new DefaultMutableTreeNode("Technology");
        technology.add(new DefaultMutableTreeNode("AAPL - Apple Inc"));
        technology.add(new DefaultMutableTreeNode("MSFT - Microsoft Corp"));
        technology.add(new DefaultMutableTreeNode("GOOGL - Alphabet Inc"));
        technology.add(new DefaultMutableTreeNode("NVDA - NVIDIA Corp"));
        technology.add(new DefaultMutableTreeNode("META - Meta Platforms"));
        technology.add(new DefaultMutableTreeNode("AMZN - Amazon.com Inc"));
        technology.add(new DefaultMutableTreeNode("TSLA - Tesla Inc"));
        technology.add(new DefaultMutableTreeNode("AMD - Advanced Micro Devices"));
        root.add(technology);

        DefaultMutableTreeNode financials = new DefaultMutableTreeNode("Financials");
        financials.add(new DefaultMutableTreeNode("JPM - JPMorgan Chase"));
        financials.add(new DefaultMutableTreeNode("BAC - Bank of America"));
        financials.add(new DefaultMutableTreeNode("GS - Goldman Sachs"));
        financials.add(new DefaultMutableTreeNode("MS - Morgan Stanley"));
        financials.add(new DefaultMutableTreeNode("WFC - Wells Fargo"));
        financials.add(new DefaultMutableTreeNode("C - Citigroup"));
        root.add(financials);

        DefaultMutableTreeNode healthcare = new DefaultMutableTreeNode("Healthcare");
        healthcare.add(new DefaultMutableTreeNode("JNJ - Johnson & Johnson"));
        healthcare.add(new DefaultMutableTreeNode("UNH - UnitedHealth Group"));
        healthcare.add(new DefaultMutableTreeNode("PFE - Pfizer Inc"));
        healthcare.add(new DefaultMutableTreeNode("ABBV - AbbVie Inc"));
        healthcare.add(new DefaultMutableTreeNode("MRK - Merck & Co"));
        root.add(healthcare);

        DefaultMutableTreeNode energy = new DefaultMutableTreeNode("Energy");
        energy.add(new DefaultMutableTreeNode("XOM - ExxonMobil"));
        energy.add(new DefaultMutableTreeNode("CVX - Chevron Corp"));
        energy.add(new DefaultMutableTreeNode("COP - ConocoPhillips"));
        energy.add(new DefaultMutableTreeNode("SLB - Schlumberger"));
        root.add(energy);

        DefaultMutableTreeNode etfs = new DefaultMutableTreeNode("ETFs");
        etfs.add(new DefaultMutableTreeNode("SPY - SPDR S&P 500"));
        etfs.add(new DefaultMutableTreeNode("QQQ - Invesco QQQ Trust"));
        etfs.add(new DefaultMutableTreeNode("IWM - iShares Russell 2000"));
        etfs.add(new DefaultMutableTreeNode("DIA - SPDR Dow Jones"));
        root.add(etfs);

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setName("instrumentTree");
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setToggleClickCount(0);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(this);
        tree.addKeyListener(this);
        tree.addMouseListener(this);

        scrollPane = new JScrollPane();
        scrollPane.setName("instrumentScrollPane");
        scrollPane.getViewport().add(tree);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    public void valueChanged(TreeSelectionEvent event) {
        TreePath path = event.getPath();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        log.info("[" + node.toString() + "] selected...");
    }

    public void keyTyped(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {}

    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == 10) {
            log.info("[ENTER] key intercepted...");
            DefaultMutableTreeNode node = getSelectedNode();
            if (node != null) {
                if (!node.isLeaf()) {
                    buildQuoteScreen();
                } else {
                    buildOrderTicket(node);
                }
            }
        }
    }

    protected void buildQuoteScreen() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (!(node == null) && (!node.isLeaf())) {
            log.info("[" + node.toString() + "] entered...");
            JInternalFrame frame = componentManager.getQuoteFrame(node.toString());
            frame.setVisible(true);
        }
    }

    protected void buildOrderTicket(DefaultMutableTreeNode node) {
        String nodeText = node.toString();
        int dashIndex = nodeText.indexOf(" - ");
        if (dashIndex < 0) return;
        String symbol = nodeText.substring(0, dashIndex).trim();

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent == null) return;
        String sectorName = parent.toString();

        String[] quoteData = QuotePanel.findQuoteData(sectorName, symbol);
        if (quoteData == null) {
            log.info("No quote data found for " + symbol + " in " + sectorName);
            return;
        }

        log.info("Opening order ticket for " + symbol + "...");
        Order order = componentManager.getOrder();
        order.setBenchmarkData(quoteData);
        order.fillControls();
        order.setVisible(true);
    }

    protected DefaultMutableTreeNode getSelectedNode() {
        TreePath selPath = tree.getSelectionPath();
        if (selPath != null)
            return (DefaultMutableTreeNode) selPath.getLastPathComponent();
        return null;
    }

    public void mouseClicked(MouseEvent e) {
        final TreePath clickedPath = tree.getPathForLocation(e.getX(), e.getY());
        if (clickedPath == null) return;

        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) clickedPath.getLastPathComponent();

        if (e.getClickCount() >= 2) {
            // Double click: cancel any pending single-click timer
            if (singleClickTimer != null && singleClickTimer.isRunning()) {
                singleClickTimer.stop();
            }
            if (!node.isLeaf()) {
                // Double click on sector: open quote screen (no toggle)
                log.info("Loading quote screen...");
                buildQuoteScreen();
            } else {
                // Double click on leaf: open order ticket
                log.info("Loading order ticket...");
                buildOrderTicket(node);
            }
        } else if (e.getClickCount() == 1) {
            if (!node.isLeaf()) {
                // Single click on sector: start timer to toggle expand/collapse
                if (singleClickTimer != null && singleClickTimer.isRunning()) {
                    singleClickTimer.stop();
                }
                singleClickTimer = new Timer(300, new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        if (tree.isExpanded(clickedPath)) {
                            tree.collapsePath(clickedPath);
                        } else {
                            tree.expandPath(clickedPath);
                        }
                    }
                });
                singleClickTimer.setRepeats(false);
                singleClickTimer.start();
            }
        }
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
}
