package com.mcd.dub.intellij.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DataSourceService extends Disposable {

    static DataSourceService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, DataSourceService.class);
    }

    String buildConnectionPool(@NotNull List<Object> connectionSettings, char[] dbPassword);

    Connection getConnectionFromPool(@NotNull String poolId) throws SQLException;

    void addPoolsListener(PropertyChangeListener propertyChangeListener);

}
