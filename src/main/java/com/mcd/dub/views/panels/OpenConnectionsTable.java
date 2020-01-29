package com.mcd.dub.views.panels;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.table.JBTable;
import com.mcd.dub.views.models.OpenConnectionsModel;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.List;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

//TODO - Move Pool info to this table as each pool is tied to a database.
public class OpenConnectionsTable extends JBTable {

    private final JBMenuItem closeConnection = new JBMenuItem("Close Connection");

    OpenConnectionsTable() {
        setName("OpenConnectionsTable");
        setDoubleBuffered(true);
        setEnableAntialiasing(true);
        setAutoCreateRowSorter(true);
        setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
        getTableHeader().setReorderingAllowed(false);
        setFillsViewportHeight(true);
        setStriped(true);
        setSelectionMode(SINGLE_SELECTION);
        setModel(new OpenConnectionsModel());
        removeColumn(getColumnModel().getColumn(6));
        setComponentPopupMenu(new JBPopupMenu());
        this.getComponentPopupMenu().add(closeConnection);
        addListeners();
    }

    private void addListeners() {
        this.getComponentPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    int rowAtPoint = rowAtPoint(SwingUtilities.convertPoint(OpenConnectionsTable.this.getComponentPopupMenu(), new Point(0, 0), OpenConnectionsTable.this));
                    if (rowAtPoint > -1) {
                        setRowSelectionInterval(rowAtPoint, rowAtPoint);
                    }
                });
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) { }
        });
        closeConnection.addActionListener(actionEvent -> ApplicationManager.getApplication().invokeLater(() -> {
            Integer val = (Integer) OpenConnectionsTable.this.getModel().getValueAt(OpenConnectionsTable.this.getSelectedRow(), 6);
            OpenConnectionsTable.this.firePropertyChange(OpenConnectionsTable.class.getSimpleName(), "closeConnectionFromTable", val);
            removeFromDataMap(OpenConnectionsTable.this.getSelectedRow());
        }));
    }

    boolean addToDataMap(List<Object> databaseConnectionSettings) {
        return ((OpenConnectionsModel)getModel()).addRow(databaseConnectionSettings);
    }

    void removeFromDataMap(int rowIndex) {
        ((OpenConnectionsModel)getModel()).getDataMap().keySet().forEach(s -> ((OpenConnectionsModel)getModel()).getDataMap().get(s).remove(rowIndex));
        for (int idx = 0; idx < ((OpenConnectionsModel)getModel()).getDataMap().get("RootTabbedPaneIndex").size(); idx++) {
            if(idx >= rowIndex) {
                int oldTabIdx = (int) ((OpenConnectionsModel)getModel()).getDataMap().get("RootTabbedPaneIndex").get(idx);
                ((OpenConnectionsModel)getModel()).getDataMap().get("RootTabbedPaneIndex").set(idx, oldTabIdx-1);
                ((OpenConnectionsModel)getModel()).fireTableDataChanged();
                break;
            }
        }
    }

}
