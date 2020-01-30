package com.mcd.dub.intellij.utils;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;

import static com.intellij.notification.NotificationType.*;
import static com.mcd.dub.intellij.utils.Constants.TableHeaderColumnDisplayNames.*;

public enum DbViewerPluginUtils {
    INSTANCE;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PluginDescriptor plugin = PluginManagerCore.getLoadedPlugins().stream().filter(o -> o.getPluginId().getIdString().equals(System.getProperty("idea.pluginAccelerator.id"))).findFirst().orElseGet(null);
    private final String pluginName = plugin == null ? "NULL!" : plugin.getName();
    private final Map<NotificationType, String> notificationTypeToLoggerCategory = new HashMap<>(3);
    {
        notificationTypeToLoggerCategory.put(ERROR, ERROR.name().toLowerCase());
        notificationTypeToLoggerCategory.put(WARNING, WARNING.name().toLowerCase().substring(0, 4));
        notificationTypeToLoggerCategory.put(INFORMATION, INFORMATION.name().toLowerCase().substring(0, 4));
    }

    public void writeToEventLog(@NotNull NotificationType notificationType, @NotNull String msg, @Nullable Exception exception, boolean hideBalloon, boolean writeToLogFile) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = new Notification(pluginName, pluginName, msg, notificationType);
            Notifications.Bus.notify(notification);
            if (hideBalloon)
                notification.expire();
        });
        if(writeToLogFile || exception != null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final List<Exception> exceptions = new ArrayList<>(3);
                if(exception instanceof SQLException) {
                    SQLException sqlEx = (SQLException) exception;
                    while (sqlEx != null) {
                        exceptions.add(sqlEx);
                        sqlEx = sqlEx.getNextException();
                    }
                } else {
                    exceptions.add(exception);
                }
                exceptions.forEach(e -> {
                    try {
                        switch (notificationType) {
                            case ERROR:
                            case WARNING:
                                Logger.class.getMethod(notificationTypeToLoggerCategory.get(notificationType), String.class, Throwable.class).invoke(logger, msg, exception);
                                break;
                            default:
                                Logger.class.getMethod(notificationTypeToLoggerCategory.get(notificationType), String.class).invoke(logger, msg);
                                break;
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                        logger.error("::writeToEventLog -> Attempting to write to Log.", ex);
                    }
                });
            });
        }
    }

    public Map<String, List<Object>> getDataMap(boolean immutable) {
        final Map<String, List<Object>> dataMap = new LinkedHashMap<>(8);
        //dataMap.put("#"                          , new ArrayList<>(5));
        dataMap.put(VENDOR.getDisplayName()      , new ArrayList<>(5));
        dataMap.put(HOST.getDisplayName()        , new ArrayList<>(5));
        dataMap.put(PORT.getDisplayName()        , new ArrayList<>(5));
        dataMap.put(DATABASE.getDisplayName()    , new ArrayList<>(5));
        dataMap.put(URL.getDisplayName()        , new ArrayList<>(5));
        dataMap.put(USERNAME.getDisplayName()    , new ArrayList<>(5));
        dataMap.put("RootTabbedPaneIndex", new ArrayList<>(5));
        if(immutable) {
            return Collections.unmodifiableMap(dataMap);
        }
        return dataMap;
    }

    @Nullable
    public PluginDescriptor getPlugin() {
        return plugin;
    }

    public String getPluginName() {
        return pluginName;
    }

}
