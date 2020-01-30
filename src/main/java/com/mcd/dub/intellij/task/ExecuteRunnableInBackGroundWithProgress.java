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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

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
            Future<String> future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final int[] fib = new int[] { 1, 2, 3, 5, 8, 13 };
                int attempt = 0;
                while(thread.isAlive()) {
                    thread.interrupt();
                    LockSupport.parkNanos(fib[attempt] * 1000);
                    attempt++;
                    if(attempt > fib.length -1) {
                        break;
                    }
                }
                return "Thread isAlive() -> " + thread.isAlive();
            });
            String s = "N/A";
            try {
                s = future.get(33, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("::onCancel -> ", e);
                future.cancel(true);
            }
            //ApplicationManager.getApplication().executeOnPooledThread(thread::interrupt);
            DbViewerPluginUtils.INSTANCE.writeToEventLog(WARNING, "Task - " + this.getTitle() + " was Interrupted, Status: " + s, null, false, true);
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
