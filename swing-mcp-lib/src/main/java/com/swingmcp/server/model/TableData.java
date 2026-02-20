package com.swingmcp.server.model;

import java.util.List;

/**
 * DTO representing table data from a JTable component.
 * Includes column names, row data with pagination support.
 */
public class TableData {
    private int id;
    private String type;
    private String name;
    private int rowCount;
    private int columnCount;
    private List<String> columns;
    private List<Integer> selectedRows;
    private List<Integer> selectedColumns;
    private String sortColumn;
    private String sortOrder;
    private List<List<Object>> data;
    private int[] dataRange;
    private int totalRows;
    private boolean hasMore;
    private String foreground;
    private String background;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public int getColumnCount() { return columnCount; }
    public void setColumnCount(int columnCount) { this.columnCount = columnCount; }

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }

    public List<Integer> getSelectedRows() { return selectedRows; }
    public void setSelectedRows(List<Integer> selectedRows) { this.selectedRows = selectedRows; }

    public List<Integer> getSelectedColumns() { return selectedColumns; }
    public void setSelectedColumns(List<Integer> selectedColumns) { this.selectedColumns = selectedColumns; }

    public String getSortColumn() { return sortColumn; }
    public void setSortColumn(String sortColumn) { this.sortColumn = sortColumn; }

    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }

    public List<List<Object>> getData() { return data; }
    public void setData(List<List<Object>> data) { this.data = data; }

    public int[] getDataRange() { return dataRange; }
    public void setDataRange(int[] dataRange) { this.dataRange = dataRange; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }

    public String getForeground() { return foreground; }
    public void setForeground(String foreground) { this.foreground = foreground; }

    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }
}
