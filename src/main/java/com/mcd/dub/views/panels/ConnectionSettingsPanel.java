package com.mcd.dub.views.panels;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;

import static com.intellij.notification.NotificationType.INFORMATION;
import static com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes.SQLITE;
import static java.lang.System.lineSeparator;
import static javax.swing.JOptionPane.DEFAULT_OPTION;
import static javax.swing.JOptionPane.ERROR_MESSAGE;

public final class ConnectionSettingsPanel extends JBPanel<ConnectionSettingsPanel> {

    private JPanel rootPanel;
    JPasswordField userPassword;
    JTextField hostName, hostPort, userName, resultingUrl, databaseName;
    JComboBox<SqlDatabaseTypes> vendor;
    JComboBox<String> connectionType;
    JButton createConnectionButton, clearConnectionDetailsButton, statusButton;

    ConnectionSettingsPanel() {
        ApplicationManager.getApplication().invokeLater(new Listeners());
    }
    
    List<Object> getDatabaseSettings() {
        List<Object> fieldValues = new ArrayList<>(6);
        SqlDatabaseTypes dataBaseType = (SqlDatabaseTypes) Objects.requireNonNull(vendor.getSelectedItem());
        fieldValues.add(Objects.requireNonNull(dataBaseType));
        fieldValues.add(hostName.getText());
        fieldValues.add(hostPort.getText());
        fieldValues.add(databaseName.getText());
        fieldValues.add(resultingUrl.getText());
        fieldValues.add(userName.getText());
        return fieldValues;
    }
    
    void setDatabaseSettings(List<Object> databaseSettings) {
        databaseSettings.replaceAll(o -> o == null ? "" : o );
        ApplicationManager.getApplication().invokeLater(() -> {
            vendor.setSelectedItem(SqlDatabaseTypes.valueOf(databaseSettings.get(0).toString()));
            hostName.setText(databaseSettings.get(1).toString());
            hostPort.setText(databaseSettings.get(2).toString());
            databaseName.setText(databaseSettings.get(3).toString());
            resultingUrl.setText(databaseSettings.get(4).toString());
            userName.setText(databaseSettings.get(5).toString());
            createConnectionButton.setEnabled(true);
            userPassword.setText(""); 
        });
    }

    boolean paramsPassValidation(Project project, List<Object> databaseConnectionSettings) {
        Map<String, List<Object>> validationMap = DbViewerPluginUtils.INSTANCE.getDataMap(false);
        validationMap.remove("RootTabbedPaneIndex");
        Iterator<String> keySet = validationMap.keySet().iterator();
        databaseConnectionSettings.forEach(o -> validationMap.get(keySet.next()).add(o));
        StringBuilder errorMsg = new StringBuilder("Connection Field(s): ").append(lineSeparator());
        int initialLength = errorMsg.length();
        if(SqlDatabaseTypes.valueOf(databaseConnectionSettings.get(0).toString()).equals(SQLITE)) {
            validationMap.remove("Port");
            validationMap.remove("Username");
        }
        for (Map.Entry<String, List<Object>> fieldParameterEntry : validationMap.entrySet()) {
            if(fieldParameterEntry.getValue() == null || fieldParameterEntry.getValue().get(0).toString().isEmpty()) {
                errorMsg.append(" - ").append(fieldParameterEntry.getKey());
                errorMsg.append(lineSeparator());
            }
        }
        if(errorMsg.length() > initialLength) {
            errorMsg.append("Failed Validation!");
            errorMsg.trimToSize();
            DbViewerPluginUtils.INSTANCE.writeToEventLog(INFORMATION, "Input Error: " + errorMsg.toString(), null, true, true);
            JOptionPane.showConfirmDialog(Objects.requireNonNull(WindowManager.getInstance().getFrame(project)).getContentPane(), errorMsg.toString(), "Input Error", DEFAULT_OPTION, ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void createUIComponents() {
        rootPanel = this;
        hostName = new JBTextField("127.0.0.1");
        userName = new JBTextField();
        databaseName = new JBTextField();
        userPassword = new JPasswordField();

        vendor = new ComboBox<>(new DefaultComboBoxModel<>());
        for (SqlDatabaseTypes dbType : SqlDatabaseTypes.values()) {
            if(dbType.isEnabled()) {
                ((DefaultComboBoxModel<SqlDatabaseTypes>)vendor.getModel()).addElement(dbType);
            }
        }
        int defaultPort = ((SqlDatabaseTypes) Objects.requireNonNull(vendor.getSelectedItem())).getDefaultPort();
        if(defaultPort != -1) {
            hostPort = new JBTextField(String.valueOf(defaultPort));
        }
        resultingUrl = new JBTextField();
        setResultingUrl();

        connectionType = new ComboBox<>(new DefaultComboBoxModel<>());
        ((DefaultComboBoxModel<String>)connectionType.getModel()).addElement("DBCP");
        ((DefaultComboBoxModel<String>)connectionType.getModel()).addElement("JNDI");

        statusButton = new JButton("Ping");
        createConnectionButton = new JButton("Connect");
        clearConnectionDetailsButton = new JButton("Clear");
    }

    private void setResultingUrl() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if((Objects.requireNonNull(vendor.getSelectedItem())).equals(SQLITE)) {
                resultingUrl.setText(((SqlDatabaseTypes) Objects.requireNonNull(vendor.getSelectedItem())).getJdbcPrefix() + hostName.getText() + databaseName.getText());
            } else {
                resultingUrl.setText(((SqlDatabaseTypes) Objects.requireNonNull(vendor.getSelectedItem())).getJdbcPrefix() + hostName.getText() + ":" + hostPort.getText() + "/" + databaseName.getText());
            }
        });
    }

    private void setDefaultPort() {
        ApplicationManager.getApplication().invokeLater(() -> {
            int defaultPort = ((SqlDatabaseTypes) Objects.requireNonNull(vendor.getSelectedItem())).getDefaultPort();
            if(defaultPort != -1) {
                hostPort.setText(String.valueOf(defaultPort));
            } else {
                hostPort.setText("");
            }
        });
    }

    private class Listeners implements Runnable {
        @Override
        public void run() {
            createConnectionButton.addActionListener(e -> {
                createConnectionButton.setEnabled(false);
                ConnectionSettingsPanel.this.firePropertyChange(ConnectionSettingsPanel.class.getSimpleName(), createConnectionButton.getText(), null);
            });
            clearConnectionDetailsButton.addActionListener(e -> {
                clearConnectionDetailsButton.setEnabled(false);
                databaseName.setText("");
                userPassword.setText("");
                userName.setText("");
                vendor.setSelectedIndex(0);
                databaseName.setBackground(JBColor.WHITE);
                int defaultPort = ((SqlDatabaseTypes) Objects.requireNonNull(vendor.getSelectedItem())).getDefaultPort();
                if(defaultPort != -1) {
                    hostPort.setText(String.valueOf(defaultPort));
                }
                hostName.setText("127.0.0.1");
                setDefaultPort();
                setResultingUrl();
                ConnectionSettingsPanel.this.firePropertyChange(ConnectionSettingsPanel.class.getSimpleName(), clearConnectionDetailsButton.getText(), null);
            });
            vendor.addItemListener(itemEvent -> {
                setDefaultPort();
                if(itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem().equals(SQLITE)) {
                    hostPort.setText("");
                    userName.setText("");
                    userPassword.setText("");
                    hostPort.setEnabled(false);
                    connectionType.setEnabled(false);
                    userName.setEnabled(false);
                    userPassword.setEnabled(false);
                    connectionType.setEnabled(false);
                } else if(itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    hostPort.setEnabled(true);
                    connectionType.setEnabled(true);
                    userName.setEnabled(true);
                    userPassword.setEnabled(true);
                    connectionType.setEnabled(true);
                    userPassword.setText("");
                }
                setResultingUrl();
            });

            statusButton.addActionListener(actionEvent -> {
                statusButton.setEnabled(false);
                ConnectionSettingsPanel.this.firePropertyChange(ConnectionSettingsPanel.class.getSimpleName(), statusButton.getText(), null);
            });

            databaseName.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent keyEvent) { setResultingUrl(); }
                @Override
                public void keyPressed(KeyEvent keyEvent) { }
                @Override
                public void keyReleased(KeyEvent keyEvent) { setResultingUrl(); }
            });
            hostName.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) { setResultingUrl(); }
                @Override
                public void keyPressed(KeyEvent e) { }
                @Override
                public void keyReleased(KeyEvent e) { setResultingUrl(); }
            });
            hostPort.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) { setResultingUrl(); }
                @Override
                public void keyPressed(KeyEvent e) { }
                @Override
                public void keyReleased(KeyEvent e) { setResultingUrl(); }
            });
        }
    }

}
