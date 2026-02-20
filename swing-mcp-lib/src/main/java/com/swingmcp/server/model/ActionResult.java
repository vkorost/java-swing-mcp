package com.swingmcp.server.model;

import java.util.Map;

/**
 * DTO representing the result of an interaction action.
 * Includes success status, the action performed, and the updated component state.
 */
public class ActionResult {
    private boolean success;
    private String action;
    private String target;
    private Map<String, Object> componentState;
    private String error;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public Map<String, Object> getComponentState() { return componentState; }
    public void setComponentState(Map<String, Object> componentState) { this.componentState = componentState; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    /**
     * Create a successful action result.
     */
    public static ActionResult success(String action, String target, Map<String, Object> componentState) {
        ActionResult result = new ActionResult();
        result.success = true;
        result.action = action;
        result.target = target;
        result.componentState = componentState;
        result.error = null;
        return result;
    }

    /**
     * Create a failed action result.
     */
    public static ActionResult failure(String action, String target, String error) {
        ActionResult result = new ActionResult();
        result.success = false;
        result.action = action;
        result.target = target;
        result.componentState = null;
        result.error = error;
        return result;
    }
}
