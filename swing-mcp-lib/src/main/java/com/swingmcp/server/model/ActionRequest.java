package com.swingmcp.server.model;

/**
 * DTO representing an interaction request sent to the /action endpoint.
 * Contains the action type, target component, and action-specific parameters.
 */
public class ActionRequest {
    private String action;
    private String target;
    private String text;
    private String value;
    private Integer index;
    private Integer row;
    private String path;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Integer getIndex() { return index; }
    public void setIndex(Integer index) { this.index = index; }

    public Integer getRow() { return row; }
    public void setRow(Integer row) { this.row = row; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
