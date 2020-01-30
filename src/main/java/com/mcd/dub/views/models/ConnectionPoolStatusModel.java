package com.mcd.dub.views.models;

import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;

import javax.swing.table.AbstractTableModel;
import java.util.*;

public class ConnectionPoolStatusModel extends AbstractTableModel {

    private static final Map<String, List<Object>> dataMap = new LinkedHashMap<>();
    {
        dataMap.put("Pool Type", new ArrayList<>());
        dataMap.put("Status", new ArrayList<>());
        dataMap.put("# Active", new ArrayList<>());
        dataMap.put("# Inactive", new ArrayList<>());
        fireTableStructureChanged();
        List<List<Object>> initialRows = new ArrayList<>();
        for (SqlDatabaseTypes sqlDatabaseType : SqlDatabaseTypes.values()) {
            if(sqlDatabaseType.isEnabled()) {
                initialRows.add(Arrays.asList(sqlDatabaseType, "Not Started", 0, 0));
            }
        }
        bulkAddRow(initialRows);
    }

    @Override
    public int getRowCount() {
        return dataMap.values().stream().max(Comparator.comparing(List::size)).orElse(Collections.emptyList()).size();
    }

    @Override
    public int getColumnCount() {
        return dataMap.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return Collections.list(Collections.enumeration(dataMap.entrySet())).get(columnIndex).getValue().get(rowIndex);
    }

    @Override
    public final String getColumnName(int columnIndex) {
        return Collections.list(Collections.enumeration(dataMap.entrySet())).get(columnIndex).getKey();
    }

    private void bulkAddRow(List<List<Object>> newRows) {
        List<String> dataMapKeySet = new ArrayList<>(dataMap.keySet());
        newRows.forEach(newRow -> {
            for (int idx = 0; idx < dataMap.keySet().size(); idx++) {
                dataMap.get(dataMapKeySet.get(idx)).add(newRow.get(idx));
            }
        });
        fireTableDataChanged();
    }

    public final boolean addRow(List<Object> newRow) {
        boolean success = false;
        List<String> dataMapKeySet = new ArrayList<>(dataMap.keySet());
        for (int idx = 0; idx < dataMap.keySet().size(); idx++) {
            success = dataMap.get(dataMapKeySet.get(idx)).add(newRow.get(idx));
        }
        fireTableDataChanged();
        return success;
    }

    public Map<String, List<Object>> getDataMap() {
        return Collections.unmodifiableMap(dataMap);
    }
}
