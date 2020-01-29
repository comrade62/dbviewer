package com.mcd.dub.intellij.utils;

import org.apache.derby.jdbc.ClientDriver;
import org.sqlite.JDBC;

import java.sql.Driver;

import static com.mcd.dub.intellij.utils.Constants.DB_TYPE.NO_SQL;
import static com.mcd.dub.intellij.utils.Constants.DB_TYPE.SQL;

public class Constants {

    public enum DB_TYPE {
        SQL, NO_SQL,
    }

    //TODO - NoSql ConnectionsSettings Card and Accessor.
    public enum NoSqlDatabaseTypes {
        CASSANDRA    (NO_SQL, "", null, -1, false),
        MONGO_DB     (NO_SQL, "", null, -1, false),
        REDIS        (NO_SQL, "", null, -1, false);

        private final DB_TYPE dbType;
        private final boolean enabled;

        NoSqlDatabaseTypes(DB_TYPE dbType, String host, Class<?> driverClass, int defaultPort, boolean enabled) {
            this.dbType = dbType;
            this.enabled = enabled;
        }

        public DB_TYPE getDbType() {
            return dbType;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    public enum protocols {
        FILE, HTTP, HTTPS
    }

    //TODO - Chec if below classes are on cp before adding to map
    public enum SqlDatabaseTypes {
        SQL_SERVER  (SQL, "jdbc:microsoft:sqlserver://", com.microsoft.sqlserver.jdbc.SQLServerDriver.class, 1433, true),//TODO - Test.
        ORACLE      (SQL, "jdbc:oracle:thin:@", null, -1, false),//TODO
        ACCESS      (SQL, "jdbc:ucanaccess://", null, -1, false),//TODO
        EXCEL       (SQL, "jdbc:excel://", null, -1, false), //TODO - write jdbc driver
        DERBY       (SQL, "jdbc:derby://" ,     ClientDriver.class, 1527, true),// TODO - Fix
        MARIA_DB    (SQL, "jdbc:mariadb://",    org.mariadb.jdbc.Driver.class, 3306, true),
        POSTGRESQL  (SQL, "jdbc:postgresql://", org.postgresql.Driver.class, 5432, true), //TODO - Fix masses of exceptions!
        MY_SQL      (SQL, "jdbc:mysql://" ,     com.mysql.jdbc.Driver.class, 3306, true),
        H2          (SQL, "jdbc:h2:-tcp://" ,   org.h2.Driver.class, 8082, true), //TODO - Test
        SQLITE      (SQL, "jdbc:sqlite:" ,      JDBC.class, -1, true),
        HSQLDB      (SQL, "jdbc:hsqldb:hsql://", null, 9001, false); // TODO


        private String MS_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver.class";

        private final int defaultPort;
        private final String jdbcPrefix;
        private final Class<? extends java.sql.Driver> driverClass;
        private final DB_TYPE dbType;
        private final boolean enabled;

        SqlDatabaseTypes(DB_TYPE dbType, String jdbcPrefix, Class<? extends Driver> driverClass, int defaultPort, boolean enabled) {
            this.dbType = dbType;
            this.jdbcPrefix = jdbcPrefix;
            this.driverClass = driverClass;
            this.defaultPort = defaultPort;
            this.enabled = enabled;
        }

        public String getJdbcPrefix() {
            return jdbcPrefix;
        }

        public Class<? extends Driver> getDriverClass() {
            return driverClass;
        }

        public int getDefaultPort() {
            return defaultPort;
        }

        public DB_TYPE getDbType() {
            return dbType;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    public enum GenericSqlStrings {
        ROW_COUNT("rowcount"),
        ROW_COUNT_QUERY("SELECT COUNT(*) AS rowcount FROM "),
        SELECT_ALL_FROM_TABLE("select * from ");

        private final String sqlString;

        GenericSqlStrings(String s) {
            sqlString = s;
        }

        @Override
        public String toString() {
            return this.sqlString;
        }
    }

    public enum JDBCKeywords {
        TABLE_SCHEM, TABLE_CAT, TABLE, TABLE_NAME, VIEW, COLUMN_NAME
    }

    public enum TableHeaderColumnDisplayNames {
        VENDOR(System.getProperty("panel.connectionsettings.vendor").replace(":", "")),
        HOST(System.getProperty("panel.connectionsettings.host").replace(":", "")),
        PORT(System.getProperty("panel.connectionsettings.port").replace(":", "")),
        DATABASE(System.getProperty("panel.connectionsettings.database").replace(":", "")),
        URL(System.getProperty("panel.connectionsettings.url").replace(":", "")),
        USERNAME(System.getProperty("panel.connectionsettings.username").replace(":", ""));

        private final String displayName;

        TableHeaderColumnDisplayNames(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

}
