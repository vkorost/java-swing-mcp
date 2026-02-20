package com.swingmcp.server.model;

import java.util.Map;

/**
 * DTO representing a single node in the Swing component tree.
 * Used for flat-array serialization in the /tree endpoint response.
 */
public class ComponentNode {
    private int id;
    private String type;
    private String name;
    private Integer parent;
    private Map<String, Integer> bounds;
    private Map<String, Integer> screenBounds;
    private boolean visible;
    private boolean enabled;
    private boolean focused;
    private String text;
    private String accessibleRole;
    private int childCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getParent() { return parent; }
    public void setParent(Integer parent) { this.parent = parent; }

    public Map<String, Integer> getBounds() { return bounds; }
    public void setBounds(Map<String, Integer> bounds) { this.bounds = bounds; }

    public Map<String, Integer> getScreenBounds() { return screenBounds; }
    public void setScreenBounds(Map<String, Integer> screenBounds) { this.screenBounds = screenBounds; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getAccessibleRole() { return accessibleRole; }
    public void setAccessibleRole(String accessibleRole) { this.accessibleRole = accessibleRole; }

    public int getChildCount() { return childCount; }
    public void setChildCount(int childCount) { this.childCount = childCount; }
}
