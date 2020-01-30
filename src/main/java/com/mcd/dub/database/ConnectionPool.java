package com.mcd.dub.database;

import com.intellij.openapi.application.ApplicationManager;
import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

    private final SqlDatabaseTypes sqlDatabaseType;
    private final GenericObjectPool genericObjectPool;
    private final PoolingDataSource poolingDataSource;
    private final PoolableConnectionFactory poolableConnectionFactory;
    private final SwingPropertyChangeSupport swingPropertyChange = new SwingPropertyChangeSupport(this, true);

    public ConnectionPool(@NotNull SqlDatabaseTypes sqlDatabaseType,
                          @NotNull ConnectionFactory connectionFactory,
                          @NotNull GenericObjectPool genericObjectPool,
                          boolean readOnly) {
        this.sqlDatabaseType = sqlDatabaseType;
        this.genericObjectPool = genericObjectPool;
        poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, genericObjectPool, null, "SELECT 1", readOnly, true);
        poolingDataSource = new PoolingDataSource(this.genericObjectPool);
    }

    String poolStatus() {
        return (genericObjectPool.getNumActive() == 0) ? "Not Started" : "Started";
    }

    int activeConnections() {
        return genericObjectPool.getNumActive();
    }

    int inActiveConnections() {
        return genericObjectPool.getNumIdle();
    }

    public void registerListenerWithPool(PropertyChangeListener propertyChangeListener) {
        if(!Arrays.asList(swingPropertyChange.getPropertyChangeListeners()).contains(propertyChangeListener)) {
            final int pcCount = swingPropertyChange.getPropertyChangeListeners().length;
            ApplicationManager.getApplication().executeOnPooledThread(() -> swingPropertyChange.addPropertyChangeListener(propertyChangeListener));
            DbViewerPluginUtils.INSTANCE.writeToEventLog(INFORMATION, "Listener: " + propertyChangeListener.getClass().getSimpleName() + " Added to Pool Service -> " + (pcCount < swingPropertyChange.getPropertyChangeListeners().length), null, true, true);
        }
    }

    public Connection getConnectionFromPool() throws SQLException {
        DbViewerPluginUtils.INSTANCE.writeToEventLog(INFORMATION, poolStatus(), null, true, true);
        Connection connection = poolingDataSource.getConnection();
        try {
            poolableConnectionFactory.validateConnection(connection);
        } catch (SQLException sqlEx) {
            logger.error("::getConnectionFromPool -> ", sqlEx);
            throw sqlEx;
        }
        updateListeners();
        return connection;
    }

    void closeConnection(Connection connection) {
        try {
            connection.close();
            updateListeners();
        } catch (SQLException e) {
            logger.error("::closeConnection -> ", e);
            DbViewerPluginUtils.INSTANCE.writeToEventLog(ERROR, "::handleClosingDBConnection -> " + e.getMessage(), e, false, true);
        }
    }

    void shutdownPool() {
        try {
            genericObjectPool.clear();
            genericObjectPool.close();
            updateListeners();
        } catch (Exception e) {
            DbViewerPluginUtils.INSTANCE.writeToEventLog(ERROR, "::shutdownPool-> " + e.getMessage(), e, false, true);
        }
    }

    public void updateListeners() {
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
