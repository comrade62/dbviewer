package com.mcd.dub.database;

import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes.SQLITE;
import static org.apache.commons.pool.impl.GenericObjectPool.DEFAULT_MAX_ACTIVE;
import static org.apache.commons.pool.impl.GenericObjectPool.DEFAULT_MAX_WAIT;
import static org.apache.commons.pool2.impl.GenericObjectPoolConfig.DEFAULT_MAX_IDLE;

public abstract class DataSource {

    protected static final Logger logger = LoggerFactory.getLogger(DataSource.class);
    private static final Map<String, ConnectionPool> connectionPools = new HashMap<>(SqlDatabaseTypes.values().length);

    protected String buildConnectionPool(@NotNull List<Object> connectionSettings, @Nullable char[] dbPassword) {
        if(!connectionPools.containsKey(connectionSettings.get(4).toString())) {
            ConnectionFactory cf = new DriverManagerConnectionFactory(connectionSettings.get(4).toString(), connectionSettings.get(5).toString(), String.valueOf(dbPassword == null ? new char[0] : dbPassword));
            boolean readOnly = true;
            if(connectionSettings.get(0).equals(SQLITE)) {
                SQLiteConfig config = new SQLiteConfig();
                config.setOpenMode(SQLiteOpenMode.READONLY);
                readOnly = false;
            }
            GenericObjectPool genericObjectPool = new GenericObjectPool();
            genericObjectPool.setMaxActive(DEFAULT_MAX_ACTIVE);
            genericObjectPool.setMaxIdle(DEFAULT_MAX_IDLE);
            genericObjectPool.setMaxWait(DEFAULT_MAX_WAIT);
            ConnectionPool poolingDataSource = new ConnectionPool(SqlDatabaseTypes.valueOf(connectionSettings.get(0).toString()), cf, genericObjectPool, readOnly);
            connectionPools.put(connectionSettings.get(4).toString(), poolingDataSource);
        }
        return connectionSettings.get(4).toString();
    }

    protected void shutDownAllPools() {
        connectionPools.forEach((sqlDatabaseTypes, connectionPool) -> {
            if(connectionPool != null) {
                connectionPool.shutdownPool();
                logger.info("::shutDown -> Pool Details: " + connectionPool.poolStatus());
            }
        });
    }

    public static Map<String, ConnectionPool> getConnectionPools() {
        return Collections.unmodifiableMap(connectionPools);
    }
}
