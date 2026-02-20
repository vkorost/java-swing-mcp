package com.swingmcp.demo;

public class PanelBuilder {

    AppLogger log;

    public PanelBuilder() {}

    public BlotterPanel buildBlotterPanel() {
        log.info("Building blotter panel...");
        BlotterPanel blotterPanel = new BlotterPanel();
        blotterPanel.setSize(1100, 250);
        blotterPanel.populatePanel();
        blotterPanel.setTitle("Blotter");
        return blotterPanel;
    }

    public QuotePanel buildQuotePanel(String title) {
        log.info("Building quote panel...");
        QuotePanel quotePanel = new QuotePanel();
        quotePanel.setSize(1100, 300);
        quotePanel.populatePanel(title);
        quotePanel.setTitle(title);
        return quotePanel;
    }

    public InstrumentPanel buildInstrumentPanel() {
        log.info("Building instrument panel...");
        InstrumentPanel instrumentPanel = new InstrumentPanel();
        instrumentPanel.setSize(300, 600);
        instrumentPanel.populatePanel();
        instrumentPanel.setTitle("Portfolios");
        return instrumentPanel;
    }
}
