package com.mcd.dub.intellij.persistence;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.mcd.dub.intellij.utils.DbViewerPluginUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@State(name="StoredConnections", storages = {@Storage("stored-conns.xml")})
public class StoredConnections implements PersistentStateComponent<StoredConnections> {

    @MapAnnotation(sortBeforeSave = false)
    public final Map<String, List<Object>> dataMap = DbViewerPluginUtils.INSTANCE.getDataMap(false);

    public void setDataMap(LinkedHashMap<String, List<Object>> dataMap) {
        this.dataMap.putAll(dataMap);
    }

    public Map<String, List<Object>> getDataMap() {
        return Collections.unmodifiableMap(dataMap);
    }

    @Nullable
    @Override
    public StoredConnections getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull StoredConnections state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    static StoredConnections getInstance() {
        return ServiceManager.getService(StoredConnections.class);
    }

}
