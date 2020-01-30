package com.mcd.dub.database;

import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;

import static com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes.SQLITE;

public abstract class DataSource {

    private static final Map<String, ConnectionPool> connectionPools = new HashMap<>(SqlDatabaseTypes.values().length);
    protected static final Logger logger = LoggerFactory.getLogger(DataSource.class);

    protected String buildConnectionPool(@NotNull List<Object> connectionSettings, @Nullable char[] dbPassword) throws SQLException {
        if(!connectionPools.containsKey(connectionSettings.get(4).toString())) {
            connectionDetailsValid(connectionSettings, dbPassword);

            boolean readOnly = true;
            if(connectionSettings.get(0).equals(SQLITE)) {
                SQLiteConfig config = new SQLiteConfig();
                config.setOpenMode(SQLiteOpenMode.READONLY);
                readOnly = false;
            }
            SqlDatabaseTypes databaseType = SqlDatabaseTypes.valueOf(connectionSettings.get(0).toString());
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName(databaseType.getDriverClass().getName());
            dataSource.setUsername(connectionSettings.get(5).toString());
            dataSource.setPassword(String.valueOf(dbPassword == null ? new char[0] : dbPassword));
            dataSource.setUrl(connectionSettings.get(4).toString());
            dataSource.setInitialSize(1);
            dataSource.setDefaultReadOnly(readOnly);

            ConnectionPool poolingDataSource = new ConnectionPool(databaseType, dataSource);
            connectionPools.put(connectionSettings.get(4).toString(), poolingDataSource);
        }
        return connectionSettings.get(4).toString();
    }

    protected void shutDownAllPools() {
        final List<SimpleImmutableEntry<ConnectionPool, Throwable>> poolsFailedToShutdown = new ArrayList<>(SqlDatabaseTypes.values().length);
        connectionPools.forEach((sqlDatabaseTypes, connectionPool) -> {
            if(connectionPool != null) {
                try {
                    connectionPool.shutdownPool();
                } catch (SQLException e) {
                    poolsFailedToShutdown.add(new SimpleImmutableEntry<>(connectionPool, e));
                }
                logger.info("::shutDown -> Pool Details: " + connectionPool.poolStatus());
            }
        });
        poolsFailedToShutdown.forEach(simpleImmutableEntry -> {
            StringWriter out = new StringWriter();
            simpleImmutableEntry.getValue().printStackTrace(new PrintWriter(out));
            logger.error(simpleImmutableEntry.getKey().toString() + System.lineSeparator() + out);
        });
    }

    protected Map<String, ConnectionPool> getConnectionPools() {
        return Collections.unmodifiableMap(connectionPools);
    }

    private void connectionDetailsValid(List<Object> connectionSettings, @Nullable char[] dbPassword) throws SQLException {
        ConnectionFactory cf = new DriverManagerConnectionFactory(connectionSettings.get(4).toString(), connectionSettings.get(5).toString(), String.valueOf(dbPassword == null ? new char[0] : dbPassword));
        cf.createConnection();
    }

}
