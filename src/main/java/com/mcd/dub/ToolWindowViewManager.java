package com.mcd.dub;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.mcd.dub.views.panels.ConnectionManagerRootPanel;
import org.jetbrains.annotations.NotNull;

public final class ToolWindowViewManager implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ApplicationManager.getApplication().invokeLater(() ->
                toolWindow.getContentManager().addContent(toolWindow.getContentManager().getFactory().createContent(new ConnectionManagerRootPanel(project), "", true))
        );
    }

}
