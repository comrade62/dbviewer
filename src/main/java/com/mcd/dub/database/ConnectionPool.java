package com.mcd.dub.database;

import com.intellij.openapi.application.ApplicationManager;
import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import static com.intellij.notification.NotificationType.ERROR;
import static com.intellij.notification.NotificationType.INFORMATION;

//TODO; Bi-Directional events between this & the view's tree and table models
public class ConnectionPool implements PropertyChangeListener {

    private final SqlDatabaseTypes sqlDatabaseType;
    private final GenericObjectPool objectPool;
    private final PoolingDataSource poolingDataSource;
    private final ConnectionFactory connectionFactory;
    private final SwingPropertyChangeSupport swingPropertyChange;

    public ConnectionPool(@NotNull SqlDatabaseTypes sqlDatabaseType, @NotNull ConnectionFactory connectionFactory, @NotNull GenericObjectPool objectPool) {
        this.sqlDatabaseType = sqlDatabaseType;
        this.connectionFactory = connectionFactory;
        this.objectPool = objectPool;
        poolingDataSource = new PoolingDataSource(objectPool);
        swingPropertyChange = new SwingPropertyChangeSupport(this, true);
    }

    void closePool() throws Exception {
        objectPool.close();
    }

    public String poolStatus() {
        return sqlDatabaseType.name() + ": Active Connections: " + objectPool.getNumActive() + ", In-Active: " + objectPool.getNumIdle();
    }

    public int activeConnections() {
        return objectPool.getNumActive();
    }

    public int inActiveConnections() {
        return objectPool.getNumIdle();
    }

    public void registerServiceListener(PropertyChangeListener propertyChangeListener) {
        if(swingPropertyChange.getPropertyChangeListeners().length  == 0 || !Arrays.asList(swingPropertyChange.getPropertyChangeListeners()).contains(propertyChangeListener)) {
            final int pcCount = swingPropertyChange.getPropertyChangeListeners().length;
            ApplicationManager.getApplication().executeOnPooledThread(() -> swingPropertyChange.addPropertyChangeListener(propertyChangeListener));
            DbViewerPluginUtils.INSTANCE.writeToEventLog(INFORMATION, "Listener: " + propertyChangeListener.getClass().getSimpleName() + " Added to Pool Service -> " + (pcCount < swingPropertyChange.getPropertyChangeListeners().length), null, true, true);
        }
    }

    public Connection getConnectionFromPool() throws SQLException {
        DbViewerPluginUtils.INSTANCE.writeToEventLog(INFORMATION, poolStatus(), null, true, true);
        Connection connection = poolingDataSource.getConnection();
        updateListeners();
        return connection;
    }

    public void closeConnection(Connection connection, SqlDatabaseTypes databaseType) {
        try {
            connection.close();
            updateListeners();
        } catch (SQLException e) {
            DbViewerPluginUtils.INSTANCE.writeToEventLog(ERROR, "::handleClosingDBConnection -> " + e.getMessage(), e, false, true);
        }
    }

    public void clearPool() {
        objectPool.clear();
        updateListeners();
    }

    public void shutdownPool() {
        try {
            objectPool.close();
            updateListeners();
        } catch (Exception e) {
            DbViewerPluginUtils.INSTANCE.writeToEventLog(ERROR, "::shutdownPool-> " + e.getMessage(), e, false, true);
        }
    }

    private void updateListeners() {
        swingPropertyChange.firePropertyChange("PoolLifeCycleEvent-" + sqlDatabaseType.name(), null, poolStatus());
        swingPropertyChange.firePropertyChange("ConnectionCountEvent-" + sqlDatabaseType.name(), activeConnections(), null);
        swingPropertyChange.firePropertyChange("ConnectionCountEvent-" + sqlDatabaseType.name(), null, inActiveConnections());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        //handle response from model
    }

    public SqlDatabaseTypes getSqlDatabaseType() {
        return sqlDatabaseType;
    }
}
