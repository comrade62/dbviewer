package com.mcd.dub.init;

import com.intellij.openapi.project.DumbAware;
import com.mcd.dub.intellij.utils.Constants.SqlDatabaseTypes;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Objects;

import static com.intellij.notification.NotificationType.ERROR;

public enum LoadPropertyFiles implements DumbAware {
    INSTANCE;

    private final Logger logger = LoggerFactory.getLogger(getDeclaringClass());

    LoadPropertyFiles() {
        loadDefaultPropertyFiles();
    }

    private void loadDefaultPropertyFiles() {
        String vmID = ManagementFactory.getRuntimeMXBean().getName();
        String[] defaultPropertyFiles = {"plugin.properties", "ui-strings.properties"};
        for (String propertyFileName : defaultPropertyFiles) {
            try(InputStream propsInputStream = getClass().getClassLoader().getResourceAsStream("/properties/" + propertyFileName)) {
                System.getProperties().load(Objects.requireNonNull(propsInputStream));
                logger.info("Property File: {} Loaded in JVM with Process PID: {}", propertyFileName, vmID.substring(0, vmID.indexOf('@')));
            } catch (IOException e) {
                logger.error("LoadPropertyFiles::loadDefaultPropertyFiles -> Loading Default Property Files -> ", e);
            }
        }
    }

    public void loadDatabaseDriverClasses() {
        for (SqlDatabaseTypes databaseType : SqlDatabaseTypes.values()) {
            try {
                if(databaseType.isEnabled()) {
                    Class.forName(databaseType.getDriverClass().getName());
                }
            } catch (ClassNotFoundException e) {
                DbViewerPluginUtils.INSTANCE.writeToEventLog(ERROR, "::loadDatabaseDriverClasses -> " + e.getMessage(), e, false, true);
            }
        }
    }

}
