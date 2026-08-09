// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.SoundInstanceLimiter;
import zombie.audio.SoundLimiterParams;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;

public final class ItemSoundManager {
    private static final ArrayList<InventoryItem> items = new ArrayList<>();
    private static final ArrayList<BaseSoundEmitter> emitters = new ArrayList<>();
    private static final ArrayList<InventoryItem> toAdd = new ArrayList<>();
    private static final ArrayList<InventoryItem> toRemove = new ArrayList<>();
    private static final ArrayList<InventoryItem> toStopItems = new ArrayList<>();
    private static final ArrayList<BaseSoundEmitter> toStopEmitters = new ArrayList<>();
    private static final SoundInstanceLimiter instanceLimiter = new SoundInstanceLimiter();
    private static final List<String> activeLimiterGroups = new ArrayList<>();
    private static final Map<String, BaseSoundEmitter> limiterEmitters = new HashMap<>();
    private static final Map<String, SoundLimiterParams> limiterParams = new HashMap<>();

    public static void addItem(InventoryItem item) {
        if (!GameServer.server) {
            if (item != null && !items.contains(item)) {
                toRemove.remove(item);
                int index = toStopItems.indexOf(item);
                if (index != -1) {
                    toStopItems.remove(index);
                    BaseSoundEmitter emitter = toStopEmitters.remove(index);
                    items.add(item);
                    emitters.add(emitter);
                } else if (!toAdd.contains(item)) {
                    toAdd.add(item);
                }
            }
        }
    }

    public static void removeItem(InventoryItem item) {
        if (!GameServer.server) {
            toAdd.remove(item);
            int index = items.indexOf(item);
            if (item != null && index != -1) {
                if (!toRemove.contains(item)) {
                    toRemove.add(item);
                }
            }
        }
    }

    public static void removeItems(ArrayList<InventoryItem> items) {
        for (int i = 0; i < items.size(); i++) {
            removeItem(items.get(i));
        }
    }

    public static void update() {
        if (!toStopItems.isEmpty()) {
            for (int i = 0; i < toStopItems.size(); i++) {
                InventoryItem item = toStopItems.get(i);
                item.stopSoundOnPlayer();
                BaseSoundEmitter emitter = toStopEmitters.get(i);
                emitter.stopAll();
                IsoWorld.instance.returnOwnershipOfEmitter(emitter);
            }

            toStopItems.clear();
            toStopEmitters.clear();
        }

        if (!toAdd.isEmpty()) {
            for (int i = 0; i < toAdd.size(); i++) {
                InventoryItem item = toAdd.get(i);

                assert !items.contains(item);

                items.add(item);
                BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter();
                IsoWorld.instance.takeOwnershipOfEmitter(emitter);
                emitters.add(emitter);
            }

            toAdd.clear();
        }

        if (!toRemove.isEmpty()) {
            for (int i = 0; i < toRemove.size(); i++) {
                InventoryItem item = toRemove.get(i);

                assert items.contains(item);

                int index = items.indexOf(item);
                items.remove(index);
                BaseSoundEmitter emitter = emitters.get(index);
                emitters.remove(index);
                toStopItems.add(item);
                toStopEmitters.add(emitter);
            }

            toRemove.clear();
        }

        instanceLimiter.startFrame();

        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            BaseSoundEmitter emitter = emitters.get(i);
            ItemContainer container = getExistingContainer(item);
            if (container != null || item.getWorldItem() != null && item.getWorldItem().getWorldObjectIndex() != -1) {
                if (item.getSoundLimiterGroupID() != null) {
                    item.registerWithSoundLimiter(instanceLimiter);
                } else {
                    item.updateSound(emitter);
                    emitter.tick();
                }
            } else {
                removeItem(item);
            }
        }

        instanceLimiter.getActiveGroups(activeLimiterGroups);

        for (int ix = 0; ix < items.size(); ix++) {
            InventoryItem item = items.get(ix);
            if (item.getSoundLimiterGroupID() != null && instanceLimiter.isClosest(item, item.getSoundLimiterGroupID())) {
                BaseSoundEmitter emitter = getEmitterForSoundLimiterGroup(item.getSoundLimiterGroupID());
                SoundLimiterParams params = getParamsForSoundLimiterGroup(item.getSoundLimiterGroupID());
                item.updateSound(emitter, params);
                emitter.tick();
            }
        }

        Iterator<Entry<String, BaseSoundEmitter>> it = limiterEmitters.entrySet().iterator();

        while (it.hasNext()) {
            Entry<String, BaseSoundEmitter> entry = it.next();
            if (!activeLimiterGroups.contains(entry.getKey())) {
                BaseSoundEmitter emitter = entry.getValue();
                emitter.stopAll();
                if (limiterParams.get(entry.getKey()) instanceof SoundLimiterParams params) {
                    params.clear();
                }

                it.remove();
                IsoWorld.instance.returnOwnershipOfEmitter(emitter);
            }
        }
    }

    private static ItemContainer getExistingContainer(InventoryItem item) {
        ItemContainer container = item.getOutermostContainer();
        if (container != null) {
            if (container.containingItem != null && container.containingItem.getWorldItem() != null) {
                if (container.containingItem.getWorldItem().getWorldObjectIndex() == -1) {
                    container = null;
                }
            } else if (container.parent != null) {
                if (container.parent.getObjectIndex() == -1
                    && container.parent.getMovingObjectIndex() == -1
                    && container.parent.getStaticMovingObjectIndex() == -1) {
                    container = null;
                }
            } else {
                container = null;
            }
        }

        return container;
    }

    static BaseSoundEmitter getEmitterForSoundLimiterGroup(String groupID) {
        BaseSoundEmitter emitter = limiterEmitters.get(groupID);
        if (emitter == null) {
            emitter = IsoWorld.instance.getFreeEmitter();
            IsoWorld.instance.takeOwnershipOfEmitter(emitter);
            limiterEmitters.put(groupID, emitter);
        }

        return emitter;
    }

    static SoundLimiterParams getParamsForSoundLimiterGroup(String groupID) {
        return limiterParams.computeIfAbsent(groupID, s -> new SoundLimiterParams());
    }

    public static void Reset() {
        items.clear();
        emitters.clear();
        toAdd.clear();
        toRemove.clear();
        toStopItems.clear();
        toStopEmitters.clear();
    }
}
