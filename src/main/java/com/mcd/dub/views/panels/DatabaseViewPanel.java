package com.mcd.dub.views.panels;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.PatchedDefaultMutableTreeNode;
import com.intellij.ui.treeStructure.Tree;
import com.mcd.dub.intellij.task.ExecuteRunnableInBackGroundWithProgress;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import com.mcd.dub.views.models.DatabaseViewModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

final class DatabaseViewPanel extends JBPanel<DatabaseViewPanel> {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseViewPanel.class);

    private final int maxRowCount;
    private final JBPanel<?> paginationPanel = new JBPanel<>();
    private final JLabel tableStats1 = new JBLabel("");
    private final DatabaseViewModel databaseViewModel;
    private final DefaultMutableTreeNode[] nodes;
    private final DefaultTreeModel treeModel;
    private final Project project;
    private final JBMenuItem addRowMenuButton = new JBMenuItem("Add Row"), deleteRowMenuButton = new JBMenuItem("Delete Row");
    private final JButton previousPageButton = new JButton("<- Previous"), nextPageButton = new JButton("Next ->");
    private final AtomicInteger resultSetPageNumber = new AtomicInteger(), maxPagesCurrentTable = new AtomicInteger();
    private final JBCheckBox paginationSwitch = new JBCheckBox("Pagination On/Off");
    private final JBPopupMenu tableMenu = new JBPopupMenu();
    private final JBTable resultsTable = new JBTable();
    private final JTree tableList;
    private DefaultMutableTreeNode selectedTreeElement;

    DatabaseViewPanel(@NotNull Project project, List<Object> databaseConnectionSettings, char[] dbPassword) throws SQLException {
        try {
            maxRowCount = Integer.parseInt(System.getProperty("plugin.dbTableViewTab.jTable.pageSize"));
        } catch (NumberFormatException numEx) {
            logger.error("DatabaseViewPanel::init -> ", numEx);
            throw numEx;
        }
        this.project = project;
        String databaseName = String.valueOf(databaseConnectionSettings.get(3));
        nodes = new DefaultMutableTreeNode[] { new PatchedDefaultMutableTreeNode(databaseName.length() == 0 ? "Root Node" : databaseName) };

        treeModel = new DefaultTreeModel(nodes[0]);
        tableList = new Tree(treeModel);
        resultsTable.setToolTipText("Click Top Node to Refresh Table List");
        this.setLayout(new BorderLayout());
        databaseViewModel = new DatabaseViewModel(project, Collections.unmodifiableList(databaseConnectionSettings), dbPassword);
    }

    JBPanel<?> createContent() {
        final DatabaseViewPanelListeners databaseViewPanelListeners = new DatabaseViewPanelListeners(this, project);
        ApplicationManager.getApplication().invokeLater(() -> {
            this.setVisible(false);
            setupTree(databaseViewPanelListeners);
            setupPaginationPanel(databaseViewPanelListeners);
            setupTable(databaseViewPanelListeners);
        });
        new ExecuteRunnableInBackGroundWithProgress("Refresh Database Table List...", false, false, project, PerformInBackgroundOption.ALWAYS_BACKGROUND, () -> { refreshTreeNodes(); ApplicationManager.getApplication().invokeLater(() -> this.setVisible(true)); }).queue();
        return this;
    }

    private void setupTree(DatabaseViewPanelListeners databaseViewPanelListeners) {
        tableList.setDoubleBuffered(true);
        tableList.addTreeSelectionListener(databaseViewPanelListeners);
        tableList.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        JBScrollPane treeScroll = new JBScrollPane(tableList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treeScroll.setPreferredSize(new Dimension(375, 300));
        treeScroll.setMinimumSize(new Dimension(250, 0));
        this.add(treeScroll, BorderLayout.WEST);
    }

    private void setupTable(DatabaseViewPanelListeners databaseViewPanelListeners) {
        resultsTable.setDoubleBuffered(true);
        resultsTable.setEnableAntialiasing(true);
        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultsTable.getTableHeader().setReorderingAllowed(false);
        resultsTable.setFillsViewportHeight(true);
        resultsTable.setShowColumns(true);
        resultsTable.setRowSelectionAllowed(true);

        JBScrollPane tableScroll = new JBScrollPane(resultsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setPreferredSize(new Dimension(400, 300));
        tableScroll.setMinimumSize(new Dimension(300, 0));

        tableMenu.add(addRowMenuButton);
        tableMenu.add(deleteRowMenuButton);

        tableMenu.addPopupMenuListener(databaseViewPanelListeners);
        resultsTable.setComponentPopupMenu(tableMenu);
        resultsTable.setModel(databaseViewModel);
        this.add(tableScroll, BorderLayout.CENTER);
    }

    private void setupPaginationPanel(DatabaseViewPanelListeners databaseViewPanelListeners) {
        BoxLayout paginationPanelLayout = new BoxLayout(paginationPanel, BoxLayout.LINE_AXIS);
        paginationPanel.setLayout(paginationPanelLayout);
        GridLayout paginationButtonsPanelLayout = new GridLayout(1, 4);
        paginationButtonsPanelLayout.setHgap(10);
        JBPanel<?> paginationButtonsPanel = new JBPanel<>();
        paginationButtonsPanel.setLayout(paginationButtonsPanelLayout);
        paginationPanel.add(paginationButtonsPanel);

        paginationButtonsPanel.add(previousPageButton);
        paginationButtonsPanel.add(nextPageButton);
        paginationButtonsPanel.add(tableStats1);
        paginationButtonsPanel.add(paginationSwitch);
        paginationSwitch.setSelected(true);
        nextPageButton.setEnabled(false);
        previousPageButton.setEnabled(false);
        paginationButtonsPanel.setMaximumSize(new Dimension(1000, 30));
        paginationPanel.setVisible(false);

        addRowMenuButton.addActionListener(databaseViewPanelListeners);
        deleteRowMenuButton.addActionListener(databaseViewPanelListeners);
        nextPageButton.addActionListener(databaseViewPanelListeners);
        previousPageButton.addActionListener(databaseViewPanelListeners);
        paginationSwitch.addActionListener(databaseViewPanelListeners);
        this.add(paginationPanel, BorderLayout.NORTH);
    }

    void alignAndMarkTableColumns() {

        resultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int rowIndex,
                                                           int columnIndex) {
                TableColumn tableColumn = table.getColumnModel().getColumn(columnIndex);
                String headerValue = tableColumn.getHeaderValue().toString();
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);

                if (((DatabaseViewModel) resultsTable.getModel()).getUniqueColumnHeaders().contains(headerValue)) {
                    component.setForeground(JBColor.BLACK);
                    component.setBackground(JBColor.LIGHT_GRAY);
                } else {
                    component.setForeground(JBColor.BLACK);
                    component.setBackground(JBColor.WHITE);
                }

                this.setToolTipText("DataType: " + ((DatabaseViewModel) resultsTable.getModel()).getColumnTypeFromHeaderName(tableColumn.getHeaderValue().toString()));
                return component;
            }
        });
        for (int columnIndex = 0; columnIndex < resultsTable.getColumnCount(); columnIndex++) {
            TableColumn tableColumn = resultsTable.getColumnModel().getColumn(columnIndex);
            TableCellRenderer tableColumnHeaderRenderer = resultsTable.getTableHeader().getDefaultRenderer();
            Component headerComponent = tableColumnHeaderRenderer.getTableCellRendererComponent(resultsTable, tableColumn.getHeaderValue(), false, false, 0, 0);

            int preferredWidth = headerComponent.getPreferredSize().width +15;
            final int maxWidth = tableColumn.getMaxWidth();

            for (int rowIndex = 0; rowIndex < resultsTable.getRowCount(); rowIndex++) {
                TableCellRenderer cellRenderer = resultsTable.getCellRenderer(rowIndex, columnIndex);
                Component component = resultsTable.prepareRenderer(cellRenderer, rowIndex, columnIndex);
                final int width = component.getPreferredSize().width + resultsTable.getIntercellSpacing().width;
                preferredWidth = Math.max(preferredWidth, width);
                if (preferredWidth >= maxWidth) {
                    preferredWidth = maxWidth;
                    break;
                }
            }
            tableColumn.setPreferredWidth(preferredWidth);
        }

    }

    void refreshTableData(@NotNull DefaultMutableTreeNode selectedElement) {
        resultSetPageNumber.set(0);
        selectedTreeElement = selectedElement;
        if (selectedElement.toString().contains("(")) {
            String tableName = selectedElement.toString().substring(0, selectedElement.toString().indexOf('(')).trim(),
                                currentTreeNodeLabel = selectedElement.toString();
            final int totalResultsInTable = Integer.parseInt(currentTreeNodeLabel.substring(currentTreeNodeLabel.indexOf("(") + 1, currentTreeNodeLabel.lastIndexOf(")")));
            try {
                maxPagesCurrentTable.set(((DatabaseViewModel) resultsTable.getModel()).refreshTableModel(tableName, totalResultsInTable));
            } catch (SQLException e) {
                logger.error("::refreshTableData -> ", e);
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                nextPageButton.setEnabled(maxPagesCurrentTable.get() > 0);
                tableStats1.setText("Showing: (" + maxRowCount + ") Rows per Page, Max Pages: " + maxPagesCurrentTable);
                //tableStats1.setText("Max Pages: " + maxPagesCurrentTable);
                ((DatabaseViewModel)resultsTable.getModel()).fireTableStructureChanged();
                alignAndMarkTableColumns();
                tableList.requestFocusInWindow();
                previousPageButton.setEnabled(false);
                paginationPanel.setVisible(totalResultsInTable > maxRowCount);
            });
        }
    }

    void refreshTreeNodes() {
        Map<String, SimpleEntry<Integer, SimpleEntry<String, String>>> tableMap = ((DatabaseViewModel) resultsTable.getModel()).getAllTables();
        LinkedList<SimpleEntry<String, Integer>> orderedDbTables = tableMap.entrySet().stream().map(entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().getKey())).sorted(Comparator.comparing(SimpleEntry::getValue, Comparator.reverseOrder())).collect(Collectors.toCollection(LinkedList::new));
        DbViewerPluginUtils.INSTANCE.writeToEventLog(NotificationType.INFORMATION, "::refreshTreeNodes -> Table List Size: " + orderedDbTables.size(), null, true, true);
        ApplicationManager.getApplication().invokeLater(() -> {
            paginationPanel.setVisible(false);
            tableList.setEnabled(false);
            for (DefaultMutableTreeNode node : nodes) {
                node.removeAllChildren();
            }
            orderedDbTables.forEach(tableEntry -> nodes[0].add(new PatchedDefaultMutableTreeNode(tableEntry.getKey() + " (" + tableEntry.getValue() + ")")));
            treeModel.reload();
            ((DatabaseViewModel) resultsTable.getModel()).flushModel();
            tableList.setEnabled(true);
            tableList.requestFocus();
        });
    }

    void destroyModel() {
        databaseViewModel.cleanUp();
    }

    private static class DatabaseViewPanelListeners implements ActionListener, PopupMenuListener, TreeSelectionListener {

        private final String refreshTableCommand = "DBTableViewButtonListener::populateTableDataFromResultSet Returned";

        private final DatabaseViewPanel databaseViewPanel;
        private final Project project;

        DatabaseViewPanelListeners(DatabaseViewPanel databaseViewPanel, Project project) {
            this.databaseViewPanel = databaseViewPanel;
            this.project = project;
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            if (e.getSource().equals(databaseViewPanel.addRowMenuButton)) {
                //TODO -
                JOptionPane.showMessageDialog(null, "Execute Add Row");
            } else if (e.getSource().equals(databaseViewPanel.deleteRowMenuButton)) {
                //TODO - Execute SQL & Remove from model. -> Remove View is now Readonly!
                if (databaseViewPanel.selectedTreeElement != null) {
                    JOptionPane.showMessageDialog(null, "Execute Delete Row");
                } else {
                    DbViewerPluginUtils.INSTANCE.writeToEventLog(
                            NotificationType.ERROR, "Selected Tree Element was NULL: ",
                            null, false, true);
                }
            } else if (e.getSource().equals(databaseViewPanel.previousPageButton)) {

                if (e.getActionCommand().equals(refreshTableCommand)) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        databaseViewPanel.alignAndMarkTableColumns();
                        ((DatabaseViewModel) databaseViewPanel.resultsTable.getModel()).fireTableDataChanged();
                        databaseViewPanel.resultsTable.setEnabled(true);
                        databaseViewPanel.previousPageButton.setEnabled(databaseViewPanel.resultSetPageNumber.get() != 0);
                        databaseViewPanel.nextPageButton.setEnabled(true);
                    });
                } else if(databaseViewPanel.resultSetPageNumber.get() > 0) {
                    databaseViewPanel.resultSetPageNumber.getAndDecrement();
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        ApplicationManager.getApplication().invokeLater(() -> databaseViewPanel.previousPageButton.setEnabled(false));
                        ((DatabaseViewModel) databaseViewPanel.resultsTable.getModel()).populateTableDataFromResultSet(databaseViewPanel.resultSetPageNumber.get());
                        databaseViewPanel.previousPageButton.getActionListeners()[0].actionPerformed(new ActionEvent(databaseViewPanel.previousPageButton, ActionEvent.ACTION_PERFORMED, refreshTableCommand));
                    });
                }

            } else if (e.getSource().equals(databaseViewPanel.nextPageButton)) {

                if (e.getActionCommand().equals(refreshTableCommand)) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        databaseViewPanel.alignAndMarkTableColumns();
                        ((DatabaseViewModel) databaseViewPanel.resultsTable.getModel()).fireTableDataChanged();
                        databaseViewPanel.resultsTable.setEnabled(true);
                        databaseViewPanel.nextPageButton.setEnabled(databaseViewPanel.resultSetPageNumber.get() != databaseViewPanel.maxPagesCurrentTable.get());
                        databaseViewPanel.previousPageButton.setEnabled(true);
                    });
                } else if (databaseViewPanel.resultSetPageNumber.get() < databaseViewPanel.maxPagesCurrentTable.get()) {
                    databaseViewPanel.resultSetPageNumber.getAndIncrement();
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        ApplicationManager.getApplication().invokeLater(() -> databaseViewPanel.nextPageButton.setEnabled(false));
                        ((DatabaseViewModel) databaseViewPanel.resultsTable.getModel()).populateTableDataFromResultSet(databaseViewPanel.resultSetPageNumber.get());
                        databaseViewPanel.nextPageButton.getActionListeners()[0].actionPerformed(new ActionEvent(databaseViewPanel.nextPageButton, ActionEvent.ACTION_PERFORMED, refreshTableCommand));
                    });
                }
            } else if(e.getSource().equals(databaseViewPanel.paginationSwitch)) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if(databaseViewPanel.paginationSwitch.isSelected()) {
                            databaseViewPanel.nextPageButton.setEnabled(true);
                            databaseViewPanel.maxPagesCurrentTable.set(1);
                        } else {
                            databaseViewPanel.nextPageButton.setEnabled(false);
                        }
                        ((DatabaseViewModel) databaseViewPanel.resultsTable.getModel()).setUsePagination(databaseViewPanel.paginationSwitch.isSelected());
                    });
                    //((DatabaseViewModel) resultsTable.getModel()).populateTableDataFromResultSet(resultSetPageNumber);
                    // nextPageButton.getActionListeners()[0].actionPerformed(new ActionEvent(nextPageButton, ActionEvent.ACTION_PERFORMED, refreshTableCommand));
                });
            } else {
                DbViewerPluginUtils.INSTANCE.writeToEventLog(
                        NotificationType.ERROR,
                        "DBTableViewButtonListener::actionPerformed - Did not recognise source",
                        null, false, true);
            }
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            ApplicationManager.getApplication().invokeLater(() -> {
                final int rowPoint = databaseViewPanel.resultsTable.rowAtPoint(SwingUtilities.convertPoint(databaseViewPanel.tableMenu, new Point(0, 0), databaseViewPanel.resultsTable));
                if (rowPoint > -1) {
                    databaseViewPanel.resultsTable.setRowSelectionInterval(rowPoint, rowPoint);
                }
            });
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) { }

        private final ThreadLocal<Integer> eventCounter = ThreadLocal.withInitial(() -> 0);

        @Override
        public void valueChanged(@NotNull TreeSelectionEvent e) {
            DefaultMutableTreeNode selectedElement = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
            ApplicationManager.getApplication().invokeLater(() -> {
                if (selectedElement.isLeaf()) {
                    databaseViewPanel.tableList.setEnabled(false);
                    databaseViewPanel.resultsTable.setEnabled(false);
                    new ExecuteRunnableInBackGroundWithProgress
                            ("Refresh Table: " + selectedElement, false, false, project, PerformInBackgroundOption.ALWAYS_BACKGROUND, () -> databaseViewPanel.refreshTableData(selectedElement)).queue();
                    databaseViewPanel.tableList.setEnabled(true);
                    databaseViewPanel.resultsTable.setEnabled(true);
                } else {
                    if (eventCounter.get() == 0) {
                        new ExecuteRunnableInBackGroundWithProgress
                                ("Refresh Database Table List...", false, false, project, PerformInBackgroundOption.ALWAYS_BACKGROUND, databaseViewPanel::refreshTreeNodes).queue();
                        eventCounter.set(eventCounter.get() + 1);
                        return;
                    }
                    eventCounter.set(0);
                }
            });
        }

    }

}
