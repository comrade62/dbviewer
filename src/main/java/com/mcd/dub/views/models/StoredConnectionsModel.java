package com.mcd.dub.views.models;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.io.*;
import java.util.*;

import static com.mcd.dub.intellij.utils.Constants.TableHeaderColumnDisplayNames.VENDOR;

public class StoredConnectionsModel extends AbstractTableModel implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(StoredConnectionsModel.class);

    private final Map<String, List<Object>> dataMap = DbViewerPluginUtils.INSTANCE.getDataMap(true);

    //TODO - Convert to intellij persistence component.
    private final String filePath = System.getProperty("idea.pluginAccelerator.dbTableViewTab.existingConnections.listLocation");

    public StoredConnectionsModel() {
        readObject();
        super.fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return dataMap.values().stream().max(Comparator.comparing(List::size)).orElse(Collections.emptyList()).size();
    }

    @Override
    public int getColumnCount() {
        return dataMap.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return Collections.list(Collections.enumeration(dataMap.entrySet())).get(columnIndex).getValue().get(rowIndex);
    }

    @Override
    public final String getColumnName(int columnIndex) {
        return Collections.list(Collections.enumeration(dataMap.entrySet())).get(columnIndex).getKey();
    }

    public final boolean hasElements() {
        return dataMap.get(VENDOR.getDisplayName()).size() > 0;
    }

    public final boolean uniqueRowCheck(@NotNull List<Object> tableRow) {
        boolean rowIsUnique = false;
        List<String> keys = new ArrayList<>(dataMap.keySet());
        for (int idx = 0; idx < dataMap.keySet().size() -1; idx++) {
            rowIsUnique = dataMap.get(keys.get(idx)).isEmpty() || !dataMap.get(keys.get(idx)).contains(tableRow.get(idx));
        }
        return rowIsUnique;
    }

    public final boolean addRow(@NotNull List<Object> newRow) {
        boolean success = false;
        List<String> dataMapKeySet = new ArrayList<>(dataMap.keySet());
        for (int idx = 0; idx < dataMap.keySet().size(); idx++) {
            //dataMap.get(dataMapKeySet.get(0)).add(dataMap.get(dataMapKeySet.get(0)).size() +1);
            success = dataMap.get(dataMapKeySet.get(idx)).add(newRow.get(idx));
        }
        super.fireTableDataChanged();
        writeObject();
        return success;
    }

    public final void removeRow(int rowIndex) {
        if(dataMap.get(VENDOR.getDisplayName()).size() < rowIndex) {
            return;
        }
        dataMap.keySet().forEach(s -> dataMap.get(s).remove(rowIndex));
        super.fireTableDataChanged();
        writeObject();
    }

    public final List<Object> getDataForRowByTabIndex(int tabIndex) {
        if(dataMap.get(VENDOR.getDisplayName()).size() < tabIndex) {
            return Collections.emptyList();
        }
        List<Object> rowDataAtIndex = new LinkedList<>();
        dataMap.values().forEach(objects -> rowDataAtIndex.add(objects.get(tabIndex)));
        return rowDataAtIndex;
    }

    private void writeObject() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try(ObjectOutputStream objOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath, false)))) {
                objOut.writeObject(dataMap);
            } catch (IOException e) {
                logger.error("ERROR " + e.getClass().getSimpleName(), e);
            }
        });
    }

    @SuppressWarnings({"unchecked", "unused", "ResultOfMethodCallIgnored"})
    private void readObject() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            //TODO - Use Path.
            File inputFile = new File(filePath);
            if(!inputFile.exists()) {
                try {
                    inputFile.toPath().getParent().toFile().mkdirs();
                    inputFile.createNewFile();
                } catch (IOException e) {
                    logger.error("ERROR " + e.getClass().getSimpleName(), e);
                }
            }

            if(inputFile.exists() && inputFile.isFile()) {
                if(inputFile.length() > 0L) {
                    try(ObjectInputStream objIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)))) {
                        Object existingData = objIn.readObject();
                        if(Map.class.isAssignableFrom(existingData.getClass())) {
                            ((Map<String, List<Object>>)existingData).forEach((s, objects) -> dataMap.get(s).addAll(objects));
                        } else {
                            logger.error("Error De-Serializing Row Data!");
                        }
                    } catch (StreamCorruptedException e) {
                        logger.error("::readObject -> Persisted data is corrupted, file will be deleted!" + e.getClass().getSimpleName(), e);
                    } catch (ClassNotFoundException | IOException e) {
                        logger.error("::readObject -> " + e.getClass().getSimpleName(), e);
                    }
                }
            } else {
                logger.error("Location: " + filePath + " is not a file!");
            }
        }, ModalityState.current());
    }

    private void readObjectNoData() throws ObjectStreamException { }

    public Map<String, List<Object>> getAvailableConnections() {
        return Collections.unmodifiableMap(dataMap);
    }

}
