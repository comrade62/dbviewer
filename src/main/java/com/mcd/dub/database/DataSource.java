package com.mcd.dub.database;

import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes.SQLITE;
import static org.apache.commons.pool.impl.GenericObjectPool.DEFAULT_MAX_ACTIVE;
import static org.apache.commons.pool.impl.GenericObjectPool.DEFAULT_MAX_WAIT;
import static org.apache.commons.pool2.impl.GenericObjectPoolConfig.DEFAULT_MAX_IDLE;

public abstract class DataSource {

    protected static final Logger logger = LoggerFactory.getLogger(DataSource.class);
    protected static Map<String, ConnectionPool> connectionPools = new HashMap<>(SqlDatabaseTypes.values().length);

    protected void buildConnectionPool(@NotNull List<Object> connectionSettings, char[] dbPassword) {
        if(!connectionPools.containsKey(connectionSettings.get(4).toString())) {
            ConnectionFactory cf = new DriverManagerConnectionFactory(connectionSettings.get(4).toString(), connectionSettings.get(5).toString(), String.valueOf(dbPassword));
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
            new PoolableConnectionFactory(cf, genericObjectPool, null, "SELECT 1", readOnly, true);
            ConnectionPool poolingDataSource = new ConnectionPool(SqlDatabaseTypes.valueOf(connectionSettings.get(0).toString()), cf, genericObjectPool);
            connectionPools.put(connectionSettings.get(4).toString(), poolingDataSource);
        }
    }

    protected void shutDownAllPools() {
        connectionPools.forEach((sqlDatabaseTypes, connectionPool) -> {
            if(connectionPool != null) {
                try {
                    connectionPool.clearPool();
                    connectionPool.closePool();
                } catch (Exception ex) {
                    logger.error("::shutDown -> ", ex);
                }
                logger.info("::shutDown -> Pool Details: " + connectionPool.poolStatus());
            }
        });
    }

}
