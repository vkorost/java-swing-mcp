package com.swingmcp.demo;

import javax.swing.JPanel;

public abstract class AbstractPanel extends JPanel {

    private String m_Title;
    private int m_Width;
    private int m_Height;

    public AbstractPanel() {
        super();
    }

    public void setTitle(String title) {
        m_Title = title;
    }

    public String getTitle() {
        return m_Title;
    }

    public void update() {
        super.revalidate();
    }

    public void setSize(int w, int h) {
        m_Width = w;
        m_Height = h;
        super.setSize(w, h);
    }

    public int getW() {
        return m_Width;
    }

    public int getH() {
        return m_Height;
    }
}
