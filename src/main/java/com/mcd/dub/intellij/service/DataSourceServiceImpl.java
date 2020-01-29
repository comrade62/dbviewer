package com.mcd.dub.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.mcd.dub.database.ConnectionPool;
import com.mcd.dub.database.DataSource;
import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.intellij.notification.NotificationType.INFORMATION;

class DataSourceServiceImpl extends DataSource implements DataSourceService {

    private static final SwingPropertyChangeSupport propertyChange = new SwingPropertyChangeSupport(DataSourceService.class, true);
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    {
        executorService.scheduleWithFixedDelay(() -> updateListeners(null, null), 120,60, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutDownAllPools();
            executorService.shutdown();
            int unKilledTasks = executorService.shutdownNow().size();
            if(unKilledTasks > 0) {
                logger.error("::shutDownEverything -> Un-Killed Tasks: "+ unKilledTasks);
            }
        }));
    }
    private final Project project;

    private DataSourceServiceImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public Connection getConnectionFromPool(@NotNull List<Object> connectionSettings, char[] dbPassword) throws SQLException {
        if(connectionPools.get(connectionSettings.get(4).toString()) == null) {
            buildConnectionPool(connectionSettings, dbPassword);
        }
        Connection connection = connectionPools.get(connectionSettings.get(4).toString()).getConnectionFromPool();
        updateListeners(connectionSettings.get(4).toString(), SqlDatabaseTypes.valueOf(connectionSettings.get(0).toString()));
        return connection;
    }

    @Override
    public void registerServiceListener(PropertyChangeListener propertyChangeListener) {
        if(propertyChange.getPropertyChangeListeners().length  == 0 || !Arrays.asList(propertyChange.getPropertyChangeListeners()).contains(propertyChangeListener)) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> propertyChange.addPropertyChangeListener(propertyChangeListener));
            DbViewerPluginUtils.INSTANCE.writeToEventLog(INFORMATION, "Listener: " + propertyChangeListener.getClass().getSimpleName() + " Added to Pool Service.", null, true, true);
        }
    }

    private void updateListeners(@Nullable String url, SqlDatabaseTypes databaseType) {
        if(url == null) {
            connectionPools.forEach((dbUrl, connectionPool) -> {
                String poolStatus = (connectionPool == null) ? "Not Started" : (connectionPool.activeConnections() == 0) ? "Stopped" : "Started" ;
                propertyChange.firePropertyChange("PoolLifeCycleEvent-" + databaseType.name(), null, poolStatus);
                propertyChange.firePropertyChange("ConnectionCountEvent-" + databaseType.name(), (connectionPool == null) ? 0 : connectionPool.activeConnections(), null);
                propertyChange.firePropertyChange("ConnectionCountEvent-" + databaseType.name(), null, (connectionPool == null) ? 0 : connectionPool.inActiveConnections());
            });
        } else {
            ConnectionPool connectionPool = connectionPools.get(url);
            String poolStatus = (connectionPool == null) ? "Not Started" : (connectionPool.activeConnections() == 0) ? "Stopped" : "Started" ;
            propertyChange.firePropertyChange("PoolLifeCycleEvent-" + databaseType.name(), null, poolStatus);
            propertyChange.firePropertyChange("ConnectionCountEvent-" + databaseType.name(), (connectionPool == null) ? 0 : connectionPool.activeConnections(), null);
            propertyChange.firePropertyChange("ConnectionCountEvent-" + databaseType.name(), null, (connectionPool == null) ? 0 : connectionPool.inActiveConnections());
        }
    }

}
