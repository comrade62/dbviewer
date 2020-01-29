package com.mcd.dub.intellij.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DataSourceService {

    static DataSourceService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, DataSourceService.class);
    }

    Connection getConnectionFromPool(@NotNull List<Object> connectionSettings, char[] dbPassword) throws SQLException;

    void registerServiceListener(PropertyChangeListener propertyChangeListener);

}
