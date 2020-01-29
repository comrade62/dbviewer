package com.mcd.dub.views.models;

import com.mcd.dub.intellij.utils.DbViewerPluginUtils;

import javax.swing.table.AbstractTableModel;
import java.util.*;

public class OpenConnectionsModel extends AbstractTableModel {

    private final Map<String, List<Object>> dataMap = DbViewerPluginUtils.INSTANCE.getDataMap(true);

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

    public final boolean addRow(List<Object> newRow) {
        boolean success = false;
        List<String> dataMapKeySet = new ArrayList<>(dataMap.keySet());
        for (int idx = 0; idx < dataMap.keySet().size(); idx++) {
            //dataMap.get(0).add(dataMap.get(0).size() +1);
            success = dataMap.get(dataMapKeySet.get(idx)).add(newRow.get(idx));
        }
        super.fireTableDataChanged();
        return success;
    }

    public Map<String, List<Object>> getDataMap() {
        return Collections.unmodifiableMap(dataMap);
    }

}
