package com.mcd.dub.intellij.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.mcd.dub.init.LoadPropertyFiles;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.intellij.notification.NotificationType.INFORMATION;

public class DbViewerProjectComponent implements ProjectComponent {

    private static final Logger logger = LoggerFactory.getLogger(DbViewerProjectComponent.class);

    private final Project project;

    public DbViewerProjectComponent(Project project) {
        this.project = project;
    }

    @Override
    public void initComponent() {
        ApplicationManager.getApplication().runReadAction(LoadPropertyFiles.INSTANCE::loadDatabaseDriverClasses);
    }

    @Override
    public void projectOpened() {
        DbViewerPluginUtils.INSTANCE.writeToEventLog(INFORMATION, DbViewerPluginUtils.INSTANCE.getPlugin().getPath().getAbsolutePath(), null, false, true);
        DbViewerPluginUtils.INSTANCE.writeToEventLog(INFORMATION,
                "Project: " + project.getName() + " Opened, O.S: " + SystemInfo.OS_NAME + " - " + SystemInfo.is64Bit, null, true, true);
    }

    @Override
    public void projectClosed() { }

}
