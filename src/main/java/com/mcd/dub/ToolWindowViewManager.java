package com.mcd.dub;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.mcd.dub.init.LoadPropertyFiles;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import com.mcd.dub.views.panels.ConnectionManagerRootPanel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ToolWindowViewManager implements ToolWindowFactory {

    private static final Logger logger = LoggerFactory.getLogger(ToolWindowViewManager.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        init(toolWindow);
        ApplicationManager.getApplication().invokeLater(() ->
                toolWindow.getContentManager().addContent(toolWindow.getContentManager().getFactory().createContent(new ConnectionManagerRootPanel(project), "", true))
        );
    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            LoadPropertyFiles.INSTANCE.loadDatabaseDriverClasses();
            try {
                Class.forName(DbViewerPluginUtils.class.getName());
            } catch (ClassNotFoundException e) {
                logger.error("::createToolWindowContent -> ", e);
            }
        }, ModalityState.NON_MODAL);
    }

}
