package com.mcd.dub.views.panels;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.table.JBTable;
import com.mcd.dub.views.models.StoredConnectionsModel;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.Map;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

public class StoredConnectionsTable extends JBTable {

    private final JBMenuItem removeConnection = new JBMenuItem("Delete");

    StoredConnectionsTable() {
        setName("StoredConnectionsTable");
        setDoubleBuffered(true);
        setEnableAntialiasing(true);
        setAutoCreateRowSorter(true);
        setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
        getTableHeader().setReorderingAllowed(false);
        setFillsViewportHeight(true);
        setStriped(true);
        setSelectionMode(SINGLE_SELECTION);
        setModel(new StoredConnectionsModel());
        removeColumn(getColumnModel().getColumn(6));
        setComponentPopupMenu(new JBPopupMenu());
        this.getComponentPopupMenu().add(removeConnection);
        addListeners();
        System.out.println(ApplicationManager.getApplication().isDispatchThread());
    }

    private void addListeners() {
        this.getSelectionModel().addListSelectionListener(listSelectionEvent -> ApplicationManager.getApplication().invokeLater(() -> {
            if(!listSelectionEvent.getValueIsAdjusting()) {
                if(this.getSelectedRow() != -1) {
                    final int row = this.getSelectedRow();
                    if(((StoredConnectionsModel)this.getModel()).hasElements()) {
                        StoredConnectionsTable.this.firePropertyChange(StoredConnectionsTable.class.getSimpleName(), "getValueIsAdjusting", row);
                    }
                }
            }
        }));

        this.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) { }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                if (focusEvent.getOppositeComponent() != null && focusEvent.getOppositeComponent().equals(StoredConnectionsTable.this) && StoredConnectionsTable.this.getSelectedRow() > -1) {
                    ApplicationManager.getApplication().invokeLater(() -> StoredConnectionsTable.this.firePropertyChange(StoredConnectionsTable.class.getSimpleName(), "focusLost", null));
                }
            }
        });

        this.getComponentPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    final int rowAtPoint = rowAtPoint(SwingUtilities.convertPoint(StoredConnectionsTable.this.getComponentPopupMenu(), new Point(0, 0), StoredConnectionsTable.this));
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

        removeConnection.addActionListener(actionEvent -> ApplicationManager.getApplication().invokeLater(() -> {
            if(this.getSelectedRow() != -1) {
                if(((StoredConnectionsModel)this.getModel()).hasElements()) {
                    StoredConnectionsTable.this.firePropertyChange(StoredConnectionsTable.class.getSimpleName(), "deleteStoredConnection", this.getSelectedRow());
                    ((StoredConnectionsModel) StoredConnectionsTable.this.getModel()).fireTableDataChanged();
                }
            }
        }));

    }

    void alignTable() {
        ApplicationManager.getApplication().invokeLater(() -> {
            for (int columnIndex = 0; columnIndex < this.getColumnCount(); columnIndex++) {
                TableColumn tableColumn = this.getColumnModel().getColumn(columnIndex);
                TableCellRenderer tableColumnHeaderRenderer = this.getTableHeader().getDefaultRenderer();
                Component headerComponent = tableColumnHeaderRenderer.getTableCellRendererComponent(this, tableColumn.getHeaderValue(), false, false, 0, 0);

                int preferredWidth = headerComponent.getPreferredSize().width +15;
                final int maxWidth = tableColumn.getMaxWidth();

                for (int rowIndex = 0; rowIndex < this.getRowCount(); rowIndex++) {
                    TableCellRenderer cellRenderer = this.getCellRenderer(rowIndex, columnIndex);
                    Component component = this.prepareRenderer(cellRenderer, rowIndex, columnIndex);
                    final int width = component.getPreferredSize().width + this.getIntercellSpacing().width;
                    preferredWidth = Math.max(preferredWidth, width);
                    if (preferredWidth >= maxWidth) {
                        preferredWidth = maxWidth;
                        break;
                    }
                }
                tableColumn.setPreferredWidth(preferredWidth);
            }
        });
    }

    boolean addToDataMap(List<Object> databaseConnectionSettings) {
        return ((StoredConnectionsModel)getModel()).addRow(databaseConnectionSettings);
    }

    Map<String, List<Object>>getAvailableConnections() {
        return ((StoredConnectionsModel)getModel()).getAvailableConnections();
    }

}
