package com.mcd.dub.database;

import com.intellij.openapi.application.ApplicationManager;
import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import org.apache.commons.dbcp2.BasicDataSource;
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

public class ConnectionPool implements PropertyChangeListener {

    private static Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

    private final BasicDataSource dataSource;
    private final SqlDatabaseTypes sqlDatabaseType;
    private final SwingPropertyChangeSupport swingPropertyChange = new SwingPropertyChangeSupport(this, true);

    private boolean atLeastOneClientWasConnected = false;

    ConnectionPool(@NotNull SqlDatabaseTypes sqlDatabaseType, @NotNull BasicDataSource dataSource) {
        this.sqlDatabaseType = sqlDatabaseType;
        this.dataSource = dataSource;
        dataSource.setValidationQuery("SELECT 1");
    }

    String poolStatus() {
        return (dataSource.getNumActive() == 0) ? (atLeastOneClientWasConnected) ? "Stopped" : "Not Started" : "Started";
    }

    int activeConnections() {
        return dataSource.getNumActive();
    }

    int inActiveConnections() {
        return dataSource.getNumIdle();
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
        Connection connection = dataSource.getConnection();
        if(dataSource.getFastFailValidation()) {
            throw new SQLException("::getConnectionFromPool -> ");
        }
        updateListeners();
        atLeastOneClientWasConnected = true;
        return connection;
    }

    private void closeConnection(Connection connection) {
        try {
            connection.close();
            updateListeners();
        } catch (SQLException e) {
            logger.error("::closeConnection -> ", e);
            DbViewerPluginUtils.INSTANCE.writeToEventLog(ERROR, "::handleClosingDBConnection -> " + e.getMessage(), e, false, true);
        }
    }

    void shutdownPool() throws SQLException {
        dataSource.close();
        updateListeners();
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

    @Override
    public String toString() {
        return "ConnectionPool: " + dataSource.getUrl() +
                "Status " + poolStatus() +
                "\n{dataSource= " + dataSource +
                ", Listeners= " + swingPropertyChange.getPropertyChangeListeners().length + '}';
    }

}
