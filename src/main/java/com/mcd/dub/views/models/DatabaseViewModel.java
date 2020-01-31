package com.mcd.dub.views.models;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.mcd.dub.intellij.service.DataSourceService;
import com.mcd.dub.intellij.utils.Constants.JDBCKeywords;
import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.sql.*;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;

import static com.intellij.notification.NotificationType.ERROR;
import static com.mcd.dub.intellij.utils.Constants.GenericSqlStrings.*;
import static com.mcd.dub.intellij.utils.Constants.JDBCKeywords.*;
import static com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes.DERBY;
import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;

public final class DatabaseViewModel extends AbstractTableModel {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseViewModel.class);

    private final boolean writeSqlToEventLog = Boolean.parseBoolean(System.getProperty("plugin.dbTableViewTab.jTable.printSqlToEventLog"));
    private final String uniqueColumn = System.getProperty("plugin.dbTableViewTab.jTable.preferentialUniqueColumn");
    private final int pageSize = Integer.parseInt(System.getProperty("plugin.dbTableViewTab.jTable.pageSize"));
    private final List<String> columnHeaders = new ArrayList<>(15), uniqueColumns = new ArrayList<>(5);
    private final Map<String, Integer> currentIndexOfTablesMap = new HashMap<>();
    private final List<List<Object>> rowData = new ArrayList<>(pageSize);
    private final List<Object> databaseConnectionSettings;
    private final Project project;

    private volatile boolean modelRefreshing = false, usePagination = true;
    private volatile String currentTableName;
    private SimpleEntry<String, Integer> uniqueColumnEntry;
    private Map<String, String> columnHeadersAndTypes;
    private SQLModelTransformer sqlModelTransformer;
    private Connection connection;

    public DatabaseViewModel(@NotNull Project project, List<Object> databaseConnectionSettings, char[] dbPassword) throws SQLException, IllegalStateException {
        this.project = project;
        this.databaseConnectionSettings = databaseConnectionSettings;
        sqlModelTransformer = new SQLModelTransformer(databaseConnectionSettings);
        ServiceManager.getService(project, DataSourceService.class).buildConnectionPool(databaseConnectionSettings, dbPassword);
        connectionValid();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0 && (!(uniqueColumns.isEmpty() || columnHeaders.isEmpty()) && !uniqueColumns.contains(columnHeaders.get(columnIndex)));
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (!rowData.isEmpty()) {
            return rowData.get(rowIndex).get(columnIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return rowData.size();
    }

    @Override
    public int getColumnCount() {
        return columnHeaders.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnHeaders.get(columnIndex);
    }

    @Override
    public void setValueAt(@NotNull Object value, int rowIndex, int columnIndex) { }

    public void setUsePagination(boolean usePagination) {
        this.usePagination = usePagination;
        populateTableDataFromResultSet(0);
    }

    /**
     *
     *         fieldValues.add(Objects.requireNonNull(dataBaseType.toString()));
     *         fieldValues.add(hostName.getText());
     *         fieldValues.add(hostPort.getText());
     *         fieldValues.add(databaseName.getText());
     *         fieldValues.add(resultingUrl.getText());
     *         fieldValues.add(userName.getText());
     */
    boolean connectionValid() throws SQLException, IllegalStateException {
        connection = ServiceManager.getService(project, DataSourceService.class).getConnectionFromPool(databaseConnectionSettings.get(4).toString());
        if(connection != null) {
            try {
                return connection.isValid(60);
            } catch (SQLFeatureNotSupportedException e) {
                logger.warn("::connectionValid -> ", e);
                if(connection != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public Map<String, SimpleEntry<Integer, SimpleEntry<String, String>>> getAllTables() {
        return sqlModelTransformer.getAllTableDetails(connection);
    }

    public int refreshTableModel(String tableName, int totalRowCount) throws SQLException {
        int numberOfPages = 1;
        if(!modelRefreshing) {
            modelRefreshing = true;
            if (this.currentTableName != null) {
                flushModel();
            }
            this.currentTableName = tableName;
            currentIndexOfTablesMap.putIfAbsent(tableName, 0);
            Map<String, SimpleEntry<Connection, Map<String, SimpleEntry<String, Boolean>>>> tableModel = getTableColumnsAndConnectionByName(tableName);
            connection = tableModel.get(tableName).getKey();
            columnHeaders.addAll(tableModel.get(tableName).getValue().keySet());
            uniqueColumns.addAll(tableModel.get(tableName).getValue().entrySet().stream().filter(entry -> entry.getValue().getValue()).map(Map.Entry::getKey).collect(Collectors.toList()));
            numberOfPages = totalRowCount / pageSize;
            uniqueColumnEntry = new SimpleEntry<>(uniqueColumn, columnHeaders.indexOf(uniqueColumn));
            rowData.clear();
            columnHeadersAndTypes = tableModel.get(tableName).getValue().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getKey()));
            modelRefreshing = false;
            populateTableDataFromResultSet(1);
        }
        return numberOfPages;
    }

    public void populateTableDataFromResultSet(int pageNumber) {
        final int startRow = pageSize * (pageNumber - 1), endRow = pageSize * pageNumber;
        if (!modelRefreshing && currentTableName != null) {
            modelRefreshing = true;
            try {
                Statement stmt = connection.createStatement(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY);
                String sql = SELECT_ALL_FROM_TABLE.toString() + currentTableName + (!sqlModelTransformer.getDatabaseType().equals(DERBY) ? ";" : "");
                try (ResultSet resultSet = stmt.executeQuery(sql)) {
                    rowData.clear();
                    int idx = 0;
                    for (;;) {
                        if (resultSet.next()) {
                            if(idx >= startRow && idx <= endRow) {
                                List<Object> column = new ArrayList<>(columnHeaders.size());
                                for (int columnIndex = 1; columnIndex <= columnHeaders.size(); columnIndex++) {
                                    column.add(resultSet.getObject(columnIndex));
                                }
                                rowData.add(column);
                            }
                            idx++;
                            if(usePagination && rowData.size() == pageSize) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    int oldTableRowIndex = currentIndexOfTablesMap.get(currentTableName);
                    currentIndexOfTablesMap.replace(currentTableName, Math.signum(idx) == 1f ? oldTableRowIndex + idx : oldTableRowIndex - idx);
                }
                if (writeSqlToEventLog) {
                    DbViewerPluginUtils.INSTANCE.writeToEventLog(NotificationType.INFORMATION, "Executed SQL: '" + sql + "'; Page #:" + pageNumber + " Row Range: " + startRow + " - " + endRow , null, true, true);
                }
            } catch (SQLException e) {
                logger.error("ERROR ", e);
            }
            fireTableDataChanged();
        } else {
            DbViewerPluginUtils.INSTANCE.writeToEventLog(ERROR, "DatabaseViewModel::populateTableDataFromResultSet - TableName was NULL!", null, false, true);
        }
        modelRefreshing = false;
    }

    public String getColumnTypeFromHeaderName(String columnName) {
        return columnHeadersAndTypes.get(columnName);
    }

    public List<String> getUniqueColumnHeaders() {
        return Collections.unmodifiableList(uniqueColumns);
    }

    public void flushModel() {
        rowData.clear();
        uniqueColumns.clear();
        columnHeaders.clear();
        uniqueColumnEntry = null;
        fireTableStructureChanged();
    }

    private Map<String, SimpleEntry<Connection, Map<String, SimpleEntry<String, Boolean>>>> getTableColumnsAndConnectionByName(String tableName) throws SQLException {
        Map<String, SimpleEntry<Connection, Map<String, SimpleEntry<String, Boolean>>>> tablesColumnsAndConnection = new LinkedHashMap<>();
        if(Objects.requireNonNull(connection).getMetaData() != null) {
            try (ResultSet tablesNamesRS = sqlModelTransformer.databaseTypeResultSet(connection, tableName)) {
                if (tablesNamesRS != null && tablesNamesRS.next()) {
                    String tableCatalog = tablesNamesRS.getString(JDBCKeywords.TABLE_CAT.name()),
                            tableSchema = tablesNamesRS.getString(JDBCKeywords.TABLE_SCHEM.name());
                    Map<String, SimpleEntry<String, Boolean>> columnWithUniqueMap = sqlModelTransformer.getColumnsWithUniqueMap(connection, tableCatalog, tableSchema, tableName);
                    tablesColumnsAndConnection.put(tableName, new SimpleEntry<>(connection, columnWithUniqueMap));
                }
            } catch (SQLException e) {
                logger.error("ERROR; @ getAllTableNamesFromDatabase; failure!", e);
                throw e;
            }
        }
        return tablesColumnsAndConnection;
    }

    public void cleanUp() {
        rowData.clear();
        sqlModelTransformer = null;
    }

    private static class SQLModelTransformer {

        private final String[] TYPES = {TABLE.name(), VIEW.name()};
        private final SqlDatabaseTypes databaseType;

        private SQLModelTransformer(@NotNull List<Object> databaseConnectionSettings) {
            databaseType = SqlDatabaseTypes.valueOf(databaseConnectionSettings.get(0).toString());
        }

        private Map<String, SimpleEntry<Integer, SimpleEntry<String, String>>> getAllTableDetails(Connection conn) {
            Map<String, SimpleEntry<Integer, SimpleEntry<String, String>>> tableMap = new HashMap<>();
            try {
                if (conn != null) {
                    try (ResultSet tablesNamesRS = databaseTypeResultSet(conn, null)) {
                        if (tablesNamesRS != null) {
                            while (tablesNamesRS.next()) {
                                try {
                                    String tableName = tablesNamesRS.getString(TABLE_NAME.name()),
                                            tableCatalog = tablesNamesRS.getString(TABLE_CAT.name()),
                                            tableSchema = tablesNamesRS.getString(TABLE_SCHEM.name()),
                                            sql = ROW_COUNT_QUERY.toString() + tableName + (!databaseType.equals(DERBY) ? ";" : "");
                                    ResultSet rowCountRS = conn.createStatement(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY).executeQuery(sql);
                                    rowCountRS.next();
                                    final int rowCount = rowCountRS.getInt(ROW_COUNT.toString());
                                    tableMap.put(tableName, new SimpleEntry<>(rowCount, new SimpleEntry<>(tableCatalog, tableSchema)));
                                    rowCountRS.close();
                                } catch (SQLException e) {
                                    logger.error("SQLModelTransformer - ERROR; @ getAllTableNamesFromDatabase; failure!", e);
                                }
                            }
                        }
                    }
                } else {
                    logger.error("SQLModelTransformer - FATAL; @ getAllTableNamesFromDatabase; got a NULL Connection!");
                }
            } catch (SQLException e) {
                logger.error("SQLModelTransformer - ERROR; @ getAllTableNamesFromDatabase; failure!", e);
            }
            return tableMap;
        }

        private Map<String, SimpleEntry<String, Boolean>> getColumnsWithUniqueMap(Connection conn,
                                                                                             String tableCatalog,
                                                                                             String tableSchema,
                                                                                             String tableName) {
            Map<String, SimpleEntry<String, Boolean>> columnsWithUniqueMap = new LinkedHashMap<>();
            try {
                Statement stmt = conn.createStatement(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY);
                stmt.setFetchSize(1000);
                String sql = SELECT_ALL_FROM_TABLE.toString() +
                        tableName +
                        (!databaseType.equals(DERBY) ? ";" : "");
                ResultSet rowDataResultSet = stmt.executeQuery(sql);
                ResultSetMetaData metaData = rowDataResultSet.getMetaData();
                final int columnCount = metaData.getColumnCount();

                for (int columnIdx = 1; columnIdx <= columnCount; columnIdx++) {
                    columnsWithUniqueMap.put(metaData.getColumnName(columnIdx), new SimpleEntry<>(metaData.getColumnTypeName(columnIdx), false));
                }

                List<String> uniqueColumnsList = new ArrayList<>();
                ResultSet primaryKeyRS = conn.getMetaData().getPrimaryKeys(tableCatalog, tableSchema, tableName);
                if (primaryKeyRS.next()) {
                    uniqueColumnsList.add(primaryKeyRS.getString(COLUMN_NAME.name()));
                }
                ResultSet indexColumnRS = conn.getMetaData().getIndexInfo(tableCatalog, tableSchema, tableName, true, true);
                while (indexColumnRS.next()) {
                    String uniqueColumnName = indexColumnRS.getString(COLUMN_NAME.name());
                    uniqueColumnsList.add(uniqueColumnName);
                }
                columnsWithUniqueMap.entrySet().stream().filter(entry -> uniqueColumnsList.contains(entry.getKey())).forEach(entry -> entry.setValue(new AbstractMap.SimpleEntry<>(entry.getValue().getKey(), true)));
            } catch (SQLException e) {
                logger.error("SQLModelTransformer - ERROR! - On Table: " + tableName + " trying to get Unique Columns", e);
            }
            logger.info("SQLModelTransformer - On Table: " + tableName + " Unique Columns; " + StringUtils.join(columnsWithUniqueMap.entrySet().stream().filter(e -> e.getValue().getValue()).collect(Collectors.toList()), ", "));
            return columnsWithUniqueMap;
        }

        @Nullable
        private ResultSet databaseTypeResultSet(@NotNull Connection connection, @Nullable String tableName) throws SQLException {
            /*try {
                //Check connection is usuable.
            }catch (SQLNonTransientConnectionException sqlnotrans) {

            }*/
            switch (databaseType) {
            /*case MS_SQL_SERVER:
            case ORACLE:
            case MONGO_DB:
            case REDIS:
                return null;*/
                case H2:
                    return connection.getMetaData().getTables(null, null, tableName, TYPES);
                case DERBY:
                    return connection.getMetaData().getTables(null, null, tableName, null);
                case SQLITE:
                case MARIA_DB:
                case MY_SQL:
                case POSTGRESQL:
                    return connection.getMetaData().getTables(null, null, tableName == null ? "%" : tableName, TYPES);
                default:
                    DbViewerPluginUtils.INSTANCE.writeToEventLog(ERROR, "::databaseTypeResultSet -> Could not determine resultset configuration for: " + databaseType.getJdbcPrefix(), null, false, true);
                    return null;
            }

        }

        private SqlDatabaseTypes getDatabaseType() {
            return databaseType;
        }

    }

}