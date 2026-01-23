// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptManager;

public class DictionaryDataClient extends DictionaryData {
    @Override
    protected boolean isClient() {
        return true;
    }

    @Override
    protected <T extends DictionaryInfo<?>> void parseInfoLoadList(Map<String, T> infoLoadList) throws WorldDictionaryException {
    }

    @Override
    protected <T extends DictionaryInfo<?>> void parseCurrentInfoSet(Map<String, T> infoLoadList) throws WorldDictionaryException {
        for (Entry<String, T> entry : infoLoadList.entrySet()) {
            DictionaryInfo<?> info = entry.getValue();
            boolean registered = false;
            if (info instanceof ItemInfo itemInfo) {
                if (!itemInfo.removed && itemInfo.scriptItem == null) {
                    itemInfo.scriptItem = ScriptManager.instance.getSpecificItem(itemInfo.fullType);
                }

                if (itemInfo.scriptItem != null) {
                    itemInfo.scriptItem.setRegistry_id(itemInfo.registryId);
                    itemInfo.scriptItem.setModID(itemInfo.modId);
                    itemInfo.isLoaded = true;
                    registered = true;
                }
            } else {
                if (Core.debug) {
                    DebugLog.General.warn("--------------------------------------------------------");
                    DebugLog.General.warn("-----   Reminder purge entity script storing   ---------");
                    DebugLog.General.warn("--------------------------------------------------------");
                    DebugLog.General.printStackTrace();
                }

                EntityInfo entityInfo = (EntityInfo)info;
                if (!entityInfo.removed && entityInfo.entityScript == null) {
                    entityInfo.entityScript = ScriptManager.instance.getSpecificEntity(entityInfo.fullType);
                }

                if (entityInfo.entityScript != null) {
                    entityInfo.entityScript.setRegistry_id(entityInfo.registryId);
                    entityInfo.entityScript.setModID(entityInfo.modId);
                    entityInfo.isLoaded = true;
                    registered = true;
                }
            }

            if (!registered && !info.removed) {
                DebugLog.General.error("Warning client has no script for dictionary info: " + info.fullType);
            }
        }
    }

    @Override
    protected void parseObjectNameLoadList(List<String> objNameLoadList) throws WorldDictionaryException {
    }

    @Override
    protected void backupCurrentDataSet() throws IOException {
    }

    @Override
    protected void deleteBackupCurrentDataSet() throws IOException {
    }

    @Override
    protected void createErrorBackups() {
    }

    @Override
    protected void load() throws IOException, WorldDictionaryException {
    }

    @Override
    protected void save() throws IOException, WorldDictionaryException {
    }

    @Override
    protected void saveToByteBuffer(ByteBuffer bb) throws IOException {
    }
}
