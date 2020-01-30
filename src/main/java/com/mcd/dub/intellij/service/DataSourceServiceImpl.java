package com.mcd.dub.intellij.service;

import com.intellij.openapi.project.Project;
import com.mcd.dub.database.DataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class DataSourceServiceImpl extends DataSource implements DataSourceService {

    private static final SwingPropertyChangeSupport propertyChange = new SwingPropertyChangeSupport(DataSourceService.class, true);
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    {
        executorService.scheduleWithFixedDelay(() -> updateListeners(null), 120,60, TimeUnit.SECONDS);
    }
    private final Project project;
    private final List<PropertyChangeListener> listeners = new ArrayList<>();

    private DataSourceServiceImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String buildConnectionPool(@NotNull List<Object> connectionSettings, char[] dbPassword) {
        String poolUrl = super.buildConnectionPool(connectionSettings, dbPassword);
        listeners.forEach(connectionPools.get(poolUrl)::registerListenerWithPool);
        updateListeners(null);
        return poolUrl;
    }

    @Override
    public Connection getConnectionFromPool(@NotNull String poolUrl) throws SQLException {
        Connection connection = connectionPools.get(poolUrl).getConnectionFromPool();
        //updateListeners(poolUrl);
        return connection;
    }

    @Override
    public void addPoolsListener(PropertyChangeListener propertyChangeListener) {
        if(!Arrays.asList(propertyChange.getPropertyChangeListeners()).contains(propertyChangeListener)) {
            listeners.add(propertyChangeListener);
        }
    }

    @Override
    public void dispose() {
        shutDownAllPools();
        executorService.shutdown();
        int unKilledTasks = executorService.shutdownNow().size();
        if(unKilledTasks > 0) {
            logger.error("::shutDownEverything -> Un-Killed Tasks: "+ unKilledTasks);
        }
    }

    private void updateListeners(@Nullable String poolUrl) {
        if(poolUrl == null) {
            connectionPools.forEach((dbUrl, connectionPool) -> connectionPool.updateListeners());
        } else {
            connectionPools.get(poolUrl).updateListeners();
        }
    }

}
