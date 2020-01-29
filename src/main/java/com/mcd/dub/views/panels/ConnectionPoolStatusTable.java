package com.mcd.dub.views.panels;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.table.JBTable;
import com.mcd.dub.intellij.service.DataSourceService;
import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import com.mcd.dub.views.models.ConnectionPoolStatusModel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

public class ConnectionPoolStatusTable extends JBTable implements PropertyChangeListener {

    ConnectionPoolStatusTable() {
        setName("OpenConnectionsTable");
        setDoubleBuffered(true);
        setEnableAntialiasing(true);
        setAutoCreateRowSorter(true);
        setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
        getTableHeader().setReorderingAllowed(false);
        setFillsViewportHeight(true);
        setStriped(true);
        setSelectionMode(SINGLE_SELECTION);
        setModel(new ConnectionPoolStatusModel());
        setComponentPopupMenu(new JBPopupMenu());
        JBMenuItem resetPoolMenuItem = new JBMenuItem("Reset Pool");
        this.getComponentPopupMenu().add(resetPoolMenuItem);
        JBMenuItem shutdownPoolMenuItem = new JBMenuItem("Shutdown Pool");
        this.getComponentPopupMenu().add(shutdownPoolMenuItem);
    }

    void addListeners(Project project) {
        ServiceManager.getService(project, DataSourceService.class).registerServiceListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(!evt.getPropertyName().isBlank() && evt.getPropertyName().contains("-")) {
            String[] split = evt.getPropertyName().split("-");
            if(split.length == 2) {
                SqlDatabaseTypes databaseType = SqlDatabaseTypes.valueOf(split[1]);
                int idx = ((ConnectionPoolStatusModel)getModel()).getDataMap().get("Pool Type").indexOf(databaseType);
                ApplicationManager.getApplication().invokeLater(() -> {
                    switch(split[0]) {
                        case "PoolLifeCycleEvent":
                            ((ConnectionPoolStatusModel)getModel()).getDataMap().get("Status").set(idx, evt.getNewValue());
                            ((ConnectionPoolStatusModel)getModel()).fireTableDataChanged();
                            break;
                        case "ConnectionCountEvent":
                            if(evt.getOldValue() != null) {
                                ((ConnectionPoolStatusModel)getModel()).getDataMap().get("# Active").set(idx, evt.getOldValue());
                            } else if(evt.getNewValue() != null) {
                                ((ConnectionPoolStatusModel)getModel()).getDataMap().get("# Inactive").set(idx, evt.getNewValue());
                            }
                            ((ConnectionPoolStatusModel)getModel()).fireTableDataChanged();
                            break;
                        default:
                            //TODO - Log Error
                            break;
                    }
                });
            }
        }
    }

}
