package com.mcd.dub.intellij.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.intellij.notification.NotificationType.*;

public class ExecuteRunnableInBackGroundWithProgress extends Backgroundable {

    private static final Logger logger = LoggerFactory.getLogger(ExecuteRunnableInBackGroundWithProgress.class);

    private final boolean canBeCancelled, showSuccessMsg;
    private final Thread thread;
    private Exception exception;

    public ExecuteRunnableInBackGroundWithProgress(@NotNull String title,
                                                   boolean canBeCancelled,
                                                   boolean showSuccessMsg,
                                                   @Nullable Project project,
                                                   @Nullable PerformInBackgroundOption backgroundOption,
                                                   @NotNull Runnable task) {
        super(project, title, canBeCancelled, backgroundOption);
        this.canBeCancelled = canBeCancelled;
        this.showSuccessMsg = showSuccessMsg;
        thread = new Thread(task);
    }

    @Override
    public boolean shouldStartInBackground() {
        return true;
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        progressIndicator.setIndeterminate(true);
        thread.start();
        try {
            thread.join();
            progressIndicator.stop();
        } catch (InterruptedException e) {
            exception = e;
            logger.error("ExecuteRunnableInBackGroundWithProgress::run - ERROR", e);
        }
    }

    @Override
    public void onCancel() {
        if (canBeCancelled && thread != null && thread.isAlive()) {
            ApplicationManager.getApplication().executeOnPooledThread(thread::interrupt);
            DbViewerPluginUtils.INSTANCE.writeToEventLog(WARNING, "Task - " + this.getTitle() + " was Interrupted", null, false, true);
        }
    }

    @Override
    public void onSuccess() {
        if(showSuccessMsg) {
            if(exception != null) {
                DbViewerPluginUtils.INSTANCE.writeToEventLog(ERROR, "Task - " + this.getTitle() + " Exception Thrown, Task Failed!", exception, false, true);
            } else {
                DbViewerPluginUtils.INSTANCE.writeToEventLog(INFORMATION, "Task - " + this.getTitle() + " Completed Successfully", null, false, true);
            }
        }
    }

}
